#!/usr/bin/env python3
"""Safely migrate house_image paths between /img/real and /img/flickr."""
import argparse
import subprocess
from dataclasses import dataclass


CONTAINERS = ("mysql-ha-master", "mysql-ha-slave")
COUNT_SQL = """
SELECT COUNT(*),
       COALESCE(SUM(image_url LIKE '/img/real/%'), 0),
       COALESCE(SUM(image_url LIKE '/img/flickr/%'), 0),
       COALESCE(SUM(image_url LIKE '/img/seed/%'), 0),
       COALESCE(SUM(image_url LIKE 'https://loremflickr.com/%'), 0),
       COALESCE(SUM(image_url LIKE 'https://picsum.photos/%'), 0),
       COALESCE(MIN(image_id), 0),
       COALESCE(MAX(image_id), 0),
       COALESCE(SUM(CRC32(CONCAT_WS('|', image_id, house_id, sort_order, REPLACE(image_url, '/img/flickr/', '/img/real/')))), 0),
       COALESCE(BIT_XOR(CRC32(CONCAT_WS('|', image_id, house_id, sort_order, REPLACE(image_url, '/img/flickr/', '/img/real/')))), 0)
FROM house_image;
""".strip()
SAMPLE_SQL = """
SELECT image_id, house_id, sort_order, image_url FROM (
    SELECT image_id, house_id, sort_order, image_url
    FROM house_image ORDER BY image_id LIMIT 4
) AS first_rows
UNION ALL
SELECT image_id, house_id, sort_order, image_url FROM (
    SELECT image_id, house_id, sort_order, image_url
    FROM house_image ORDER BY image_id DESC LIMIT 4
) AS last_rows;
""".strip()
SLOTS = {
    1: "cover",
    2: "living",
    3: "bed",
    4: "kitchen",
}


@dataclass(frozen=True)
class Counts:
    total: int
    real: int
    flickr: int
    seed: int
    lorem_external: int
    picsum_external: int
    min_id: int
    max_id: int
    checksum_sum: int
    checksum_xor: int


def forward_sql(start_id, end_id):
    return (
        "UPDATE house_image SET image_url=REPLACE(image_url,'/img/real/','/img/flickr/') "
        f"WHERE image_id BETWEEN {start_id} AND {end_id} "
        "AND image_url LIKE '/img/real/%';"
    )


def rollback_sql(start_id, end_id):
    return (
        "UPDATE house_image SET image_url=REPLACE(image_url,'/img/flickr/','/img/real/') "
        f"WHERE image_id BETWEEN {start_id} AND {end_id} "
        "AND image_url LIKE '/img/flickr/%';"
    )


def run_mysql(container, sql):
    command = [
        "docker",
        "exec",
        "-i",
        container,
        "sh",
        "-c",
        'exec mysql -uroot -p"$MYSQL_ROOT_PASSWORD" -N apartment_db',
    ]
    result = subprocess.run(
        command,
        input=sql,
        text=True,
        check=True,
        capture_output=True,
    )
    return result.stdout.strip()


def query_counts(container):
    values = run_mysql(container, COUNT_SQL).split("\t")
    if len(values) != 10:
        raise RuntimeError(f"unexpected count output from {container}: {values!r}")
    return Counts(*[int(value) for value in values])


def query_samples(container):
    output = run_mysql(container, SAMPLE_SQL)
    rows = []
    for line in output.splitlines():
        values = line.split("\t")
        if len(values) != 4:
            raise RuntimeError(f"unexpected sample output from {container}: {values!r}")
        rows.append((int(values[0]), int(values[1]), int(values[2]), values[3]))
    return rows


def print_counts(container, counts):
    print(
        f"{container}: total={counts.total} real={counts.real} "
        f"flickr={counts.flickr} seed={counts.seed} "
        f"lorem_external={counts.lorem_external} "
        f"picsum_external={counts.picsum_external} "
        f"id={counts.min_id}..{counts.max_id}",
        flush=True,
    )


def validate_pair(master, slave):
    if master.total != slave.total:
        raise RuntimeError(
            f"master/slave totals differ: {master.total} != {slave.total}"
        )
    if (master.checksum_sum, master.checksum_xor) != (
        slave.checksum_sum, slave.checksum_xor
    ):
        raise RuntimeError("master/slave fingerprints differ")
    if master.total <= 0:
        raise RuntimeError("house_image is empty")
    for name, counts in zip(CONTAINERS, (master, slave)):
        unexpected = (
            counts.seed + counts.lorem_external + counts.picsum_external
        )
        if unexpected:
            raise RuntimeError(f"{name} contains {unexpected} unexpected image paths")
        if counts.real + counts.flickr != counts.total:
            raise RuntimeError(f"{name} contains unsupported image path prefixes")


def validate_samples(master_rows, slave_rows, prefix):
    if master_rows != slave_rows:
        raise RuntimeError("master/slave sample rows differ")
    for image_id, house_id, sort_order, image_url in master_rows:
        slot = SLOTS.get(sort_order)
        expected = (
            f"/img/{prefix}/{slot}/{house_id * 100 + sort_order}/800/600"
            if slot
            else None
        )
        if image_url != expected:
            raise RuntimeError(
                f"invalid sampled URL at image_id={image_id}: {image_url!r}"
            )


def verify_target(counts_by_container, rollback):
    for container, counts in zip(CONTAINERS, counts_by_container):
        expected = counts.real if rollback else counts.flickr
        remaining = counts.flickr if rollback else counts.real
        if expected != counts.total or remaining != 0:
            target = "/img/real/" if rollback else "/img/flickr/"
            raise RuntimeError(f"{container} did not fully migrate to {target}")


def migrate(apply=False, rollback=False, batch_size=50000):
    if apply and rollback:
        raise ValueError("--apply and --rollback are mutually exclusive")
    if batch_size <= 0:
        raise ValueError("batch_size must be positive")

    before = [query_counts(container) for container in CONTAINERS]
    for container, counts in zip(CONTAINERS, before):
        print_counts(container, counts)
    validate_pair(before[0], before[1])
    before_prefix = None
    if (
        before[0].real == before[0].total
        and before[1].real == before[1].total
    ):
        before_prefix = "real"
    elif (
        before[0].flickr == before[0].total
        and before[1].flickr == before[1].total
    ):
        before_prefix = "flickr"
    if before_prefix:
        validate_samples(
            query_samples(CONTAINERS[0]), query_samples(CONTAINERS[1]), before_prefix
        )

    if not apply and not rollback:
        print("dry-run only; pass --apply to migrate", flush=True)
        return before

    sql_builder = rollback_sql if rollback else forward_sql
    for container, counts in zip(CONTAINERS, before):
        start_id = counts.min_id
        while start_id <= counts.max_id:
            end_id = min(start_id + batch_size - 1, counts.max_id)
            run_mysql(container, sql_builder(start_id, end_id))
            print(f"{container}: migrated image_id {start_id}..{end_id}", flush=True)
            start_id = end_id + 1

    after = [query_counts(container) for container in CONTAINERS]
    for container, counts in zip(CONTAINERS, after):
        print_counts(container, counts)
    validate_pair(after[0], after[1])
    verify_target(after, rollback)
    target_prefix = "real" if rollback else "flickr"
    validate_samples(
        query_samples(CONTAINERS[0]), query_samples(CONTAINERS[1]), target_prefix
    )
    print("migration verified", flush=True)
    return after


def main():
    parser = argparse.ArgumentParser()
    group = parser.add_mutually_exclusive_group()
    group.add_argument("--apply", action="store_true")
    group.add_argument("--rollback", action="store_true")
    parser.add_argument("--batch-size", type=int, default=50000)
    args = parser.parse_args()
    migrate(apply=args.apply, rollback=args.rollback, batch_size=args.batch_size)


if __name__ == "__main__":
    main()
