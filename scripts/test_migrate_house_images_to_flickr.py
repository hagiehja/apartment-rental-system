import importlib.util
import sys
import unittest
from pathlib import Path
from unittest.mock import patch


MODULE_PATH = Path(__file__).with_name("migrate_house_images_to_flickr.py")
SPEC = importlib.util.spec_from_file_location("migration", MODULE_PATH)
migration = importlib.util.module_from_spec(SPEC)
sys.modules["migration"] = migration
SPEC.loader.exec_module(migration)


class SqlTests(unittest.TestCase):
    def test_forward_sql_replaces_only_real_prefix_in_one_id_batch(self):
        self.assertEqual(
            migration.forward_sql(1, 50000),
            "UPDATE house_image SET image_url=REPLACE(image_url,'/img/real/','/img/flickr/') "
            "WHERE image_id BETWEEN 1 AND 50000 AND image_url LIKE '/img/real/%';",
        )

    def test_rollback_sql_replaces_only_flickr_prefix(self):
        self.assertEqual(
            migration.rollback_sql(50001, 100000),
            "UPDATE house_image SET image_url=REPLACE(image_url,'/img/flickr/','/img/real/') "
            "WHERE image_id BETWEEN 50001 AND 100000 AND image_url LIKE '/img/flickr/%';",
        )


class MigrationTests(unittest.TestCase):
    def counts(self, real, flickr, total=100000):
        return migration.Counts(
            total=total,
            real=real,
            flickr=flickr,
            seed=0,
            lorem_external=0,
            picsum_external=0,
            min_id=1,
            max_id=100000,
        )

    @patch("migration.run_mysql")
    @patch("migration.query_counts")
    def test_dry_run_queries_both_databases_without_updates(self, query_counts, run_mysql):
        query_counts.side_effect = [self.counts(100000, 0), self.counts(100000, 0)]

        migration.migrate()

        self.assertEqual(query_counts.call_count, 2)
        run_mysql.assert_not_called()

    @patch("migration.run_mysql")
    @patch("migration.query_counts")
    def test_apply_updates_both_databases_in_50000_row_batches(
        self, query_counts, run_mysql
    ):
        query_counts.side_effect = [
            self.counts(100000, 0),
            self.counts(100000, 0),
            self.counts(0, 100000),
            self.counts(0, 100000),
        ]

        migration.migrate(apply=True, batch_size=50000)

        self.assertEqual(run_mysql.call_count, 4)
        updated_containers = [call.args[0] for call in run_mysql.call_args_list]
        self.assertEqual(
            updated_containers,
            [
                "mysql-ha-master",
                "mysql-ha-master",
                "mysql-ha-slave",
                "mysql-ha-slave",
            ],
        )
        for call in run_mysql.call_args_list:
            self.assertIn("image_url LIKE '/img/real/%'", call.args[1])

    @patch("migration.query_counts")
    def test_mismatched_master_slave_totals_stop_before_updates(self, query_counts):
        query_counts.side_effect = [
            self.counts(100000, 0),
            self.counts(99999, 0, total=99999),
        ]

        with self.assertRaisesRegex(RuntimeError, "totals differ"):
            migration.migrate(apply=True)


if __name__ == "__main__":
    unittest.main()
