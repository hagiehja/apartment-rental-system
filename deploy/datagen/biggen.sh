#!/bin/bash
set -e
cd /tmp
echo "=== [$(date +%T)] 清空旧数据 ==="
docker exec mysql-5.7 mysql -uroot -p123456 apartment_db -e "SET FOREIGN_KEY_CHECKS=0; TRUNCATE payment; TRUNCATE rental_order; TRUNCATE house; TRUNCATE user; SET FOREIGN_KEY_CHECKS=1; SET GLOBAL local_infile=1;" 2>/dev/null

echo "=== [$(date +%T)] 生成 CSV(80万用户/40万房源/500万订单/200万支付)==="
python3 /tmp/gen_realistic.py 800000 400000 5000000 2000000

echo "=== [$(date +%T)] 拷贝 CSV 到 mysql 容器 ==="
docker cp /tmp/users.csv mysql-5.7:/tmp/users.csv
docker cp /tmp/houses.csv mysql-5.7:/tmp/houses.csv
docker cp /tmp/orders.csv mysql-5.7:/tmp/orders.csv
docker cp /tmp/payments.csv mysql-5.7:/tmp/payments.csv

echo "=== [$(date +%T)] LOAD DATA 导入 utf8mb4 ==="
docker exec -i mysql-5.7 mysql -uroot -p123456 --local-infile=1 apartment_db 2>/dev/null << 'SQL'
SET FOREIGN_KEY_CHECKS=0;
SET autocommit=0;
LOAD DATA LOCAL INFILE '/tmp/users.csv' INTO TABLE user CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\n' (username,phone,password,role,create_time,update_time);
LOAD DATA LOCAL INFILE '/tmp/houses.csv' INTO TABLE house CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\n' (landlord_id,title,description,province,city,district,address,area,room_count,hall_count,bathroom_count,floor,total_floor,orientation,decoration,rent_type,price,payment_method,facilities,status,view_count,create_time,update_time,version);
LOAD DATA LOCAL INFILE '/tmp/orders.csv' INTO TABLE rental_order CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\n' (order_no,tenant_id,house_id,landlord_id,rent_start_date,rent_end_date,rent_months,monthly_rent,deposit,total_amount,first_payment_amount,installment_enabled,order_status,payment_status,expire_time,create_time,@pay_time) SET pay_time=NULLIF(@pay_time,'');
LOAD DATA LOCAL INFILE '/tmp/payments.csv' INTO TABLE payment CHARACTER SET utf8mb4 FIELDS TERMINATED BY ',' OPTIONALLY ENCLOSED BY '"' LINES TERMINATED BY '\n' (payment_no,order_no,user_id,amount,payment_method,payment_status,refund_amount,@success_time,create_time,update_time) SET success_time=NULLIF(@success_time,'');
COMMIT;
SET FOREIGN_KEY_CHECKS=1;
SQL

echo "=== [$(date +%T)] 验证数据量 ==="
docker exec mysql-5.7 mysql -uroot -p123456 apartment_db -e "SELECT 'user' t, COUNT(*) c FROM user UNION ALL SELECT 'house',COUNT(*) FROM house UNION ALL SELECT 'rental_order',COUNT(*) FROM rental_order UNION ALL SELECT 'payment',COUNT(*) FROM payment;" 2>/dev/null

echo "=== [$(date +%T)] 清 Redis 缓存 ==="
MASTER=$(redis-cli -h 192.168.24.129 -p 26379 sentinel get-master-addr-by-name mymaster 2>/dev/null | head -1)
MPORT=$(redis-cli -h 192.168.24.129 -p 26379 sentinel get-master-addr-by-name mymaster 2>/dev/null | sed -n 2p)
redis-cli -h $MASTER -p $MPORT FLUSHALL 2>/dev/null && echo "缓存已清"

echo "=== [$(date +%T)] 全部完成 ==="
