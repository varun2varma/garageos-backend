-- Deterministic SYNTHETIC personas for local scenario QA. Run ONLY against a disposable database (for example
-- garageos_qa) after Flyway has created the schema and one user named qa_hash exists (its password hash is reused so
-- every persona signs in with the same local-only password: QaPass#123). Never run against a shared or real database.

CREATE TEMP TABLE _h AS SELECT password_hash AS h FROM users WHERE username = 'qa_hash';

-- Garages: 1 fully branded, 2 nothing, 3 logo only (logo uploaded through the API), 4 inactive, 5 broken logo key, 6 very long name
INSERT INTO garage (garage_code, garage_name, workshop_type, number_of_bays, status, next_employee_sequence, address, city, state, pincode, latitude, longitude, created_at, updated_at, brand_primary_color, brand_secondary_color) VALUES
 ('QA001','Aurora Auto Works','GENERAL_SERVICE',3,'ACTIVE',1,'12 MG Road','Bengaluru','Karnataka','560001',12.9750000,77.5900000,now(),now(),'#0F766E','#F59E0B'),
 ('QA002','Bluebird Garage','GENERAL_SERVICE',2,'ACTIVE',1,'5 Brigade Road','Bengaluru','Karnataka','560001',12.9800000,77.6000000,now(),now(),NULL,NULL),
 ('QA003','Cedar Motors','GENERAL_SERVICE',2,'ACTIVE',1,'9 Church Street','Bengaluru','Karnataka','560001',12.9700000,77.6050000,now(),now(),NULL,NULL),
 ('QA004','Dormant Workshop','GENERAL_SERVICE',1,'INACTIVE',1,'1 Old Airport Road','Bengaluru','Karnataka','560017',12.9710000,77.5950000,now(),now(),NULL,NULL),
 ('QA005','Elm Street Garage','GENERAL_SERVICE',1,'ACTIVE',1,'7 Elm Street','Bengaluru','Karnataka','560002',12.9760000,77.5920000,now(),now(),'#7C3AED',NULL),
 ('QA006','The Extraordinarily Long-Named Metropolitan Automotive Engineering and Roadside Assistance Cooperative Garage Private Limited','GENERAL_SERVICE',1,'ACTIVE',1,'99 A Very Long Address Line That Keeps Going For Layout Testing Purposes, Indiranagar','Bengaluru','Karnataka','560038',12.9780000,77.5930000,now(),now(),NULL,NULL);
UPDATE garage SET logo_storage_key='garage-logos/does-not-exist.png', logo_content_type='image/png', logo_updated_at=now() WHERE garage_code='QA005';

-- Staff and admin
INSERT INTO users (username,password_hash,first_name,last_name,mobile,email,status,authentication_provider,created_at,updated_at,garage_id)
SELECT v.u,(SELECT h FROM _h),v.f,'QA',v.m,v.u||'@qa.example.com','ACTIVE','LOCAL',now(),now(),(SELECT id FROM garage WHERE garage_code=v.g)
FROM (VALUES
 ('qa_super','Super','9100000001',NULL),
 ('qa_owner1','OwnerA','9100000002','QA001'),
 ('qa_owner2','OwnerB','9100000003','QA002'),
 ('qa_owner3','OwnerC','9100000004','QA003'),
 ('qa_manager1','ManagerA','9100000005','QA001'),
 ('qa_advisor1','AdvisorA','9100000006','QA001'),
 ('qa_tech1','TechA','9100000007','QA001'),
 ('qa_plain','Plain','9100000008',NULL)
) AS v(u,f,m,g);
UPDATE garage SET owner_user_id=(SELECT id FROM users WHERE username='qa_owner1') WHERE garage_code='QA001';
UPDATE garage SET owner_user_id=(SELECT id FROM users WHERE username='qa_owner2') WHERE garage_code='QA002';
UPDATE garage SET owner_user_id=(SELECT id FROM users WHERE username='qa_owner3') WHERE garage_code='QA003';

INSERT INTO user_roles (user_id,role_id,created_at,updated_at)
SELECT u.id,r.id,now(),now() FROM (VALUES
 ('qa_super','SUPER_ADMIN'),('qa_owner1','OWNER'),('qa_owner2','OWNER'),('qa_owner3','OWNER'),
 ('qa_manager1','MANAGER'),('qa_advisor1','SERVICE_ADVISOR'),('qa_tech1','TECHNICIAN'),('qa_plain','USER')
) AS x(u,rc) JOIN users u ON u.username=x.u JOIN roles r ON r.code=x.rc;

-- Customers (user login + customer record sharing one mobile number)
INSERT INTO users (username,password_hash,first_name,last_name,mobile,email,status,authentication_provider,created_at,updated_at)
SELECT v.u,(SELECT h FROM _h),v.f,'Customer',v.m,v.u||'@qa.example.com','ACTIVE','LOCAL',now(),now()
FROM (VALUES
 ('qa_c_new','NewCust','9200000001'),('qa_c_brand','BrandCust','9200000002'),('qa_c_nologo','NoLogoCust','9200000003'),
 ('qa_c_logoonly','LogoOnlyCust','9200000004'),('qa_c_multi','MultiCust','9200000005'),('qa_c_inactive','InactiveCust','9200000006'),
 ('qa_c_badlogo','BadLogoCust','9200000007'),('qa_c_long','LongNameCust','9200000008')
) AS v(u,f,m);
INSERT INTO user_roles (user_id,role_id,created_at,updated_at)
SELECT u.id,(SELECT id FROM roles WHERE code='CUSTOMER'),now(),now() FROM users u WHERE u.username LIKE 'qa_c_%';

INSERT INTO customer (first_name,last_name,mobile_number,email,created_at)
SELECT u.first_name,u.last_name,u.mobile,u.email,now() FROM users u WHERE u.username LIKE 'qa_c_%' AND u.username <> 'qa_c_new';
-- qa_c_new deliberately has a login but NO customer record and NO garage relationship.

-- Relationships: (customer, garage, default?)
INSERT INTO customer_garage (customer_id,garage_id,source,is_default,created_at)
SELECT c.id,g.id,'STAFF_REGISTERED',x.d,now() FROM (VALUES
 ('9200000002','QA001',true),
 ('9200000003','QA002',true),
 ('9200000004','QA003',true),
 ('9200000005','QA002',true),('9200000005','QA001',false),('9200000005','QA003',false),
 ('9200000006','QA004',true),
 ('9200000007','QA005',true),
 ('9200000008','QA006',true)
) AS x(m,gc,d) JOIN customer c ON c.mobile_number=x.m JOIN garage g ON g.garage_code=x.gc;

INSERT INTO vehicle (registration_number,brand,model,variant,customer_id,created_at)
SELECT 'KA01QA'||c.id,'Maruti','Swift','VXI',c.id,now() FROM customer c WHERE c.mobile_number IN ('9200000002','9200000003');
