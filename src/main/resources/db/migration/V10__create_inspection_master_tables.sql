CREATE TABLE inspection_master
(
    id BIGSERIAL PRIMARY KEY,

    make VARCHAR(100) NOT NULL,

    model VARCHAR(100) NOT NULL,

    variant VARCHAR(100),

    fuel_type VARCHAR(30),

    transmission VARCHAR(30),

    min_year INTEGER,

    max_year INTEGER,

    min_odometer INTEGER NOT NULL,

    max_odometer INTEGER NOT NULL,

    active BOOLEAN NOT NULL DEFAULT TRUE,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE inspection_master_item
(
    id BIGSERIAL PRIMARY KEY,

    inspection_master_id BIGINT NOT NULL,

    category VARCHAR(100) NOT NULL,

    check_item VARCHAR(255) NOT NULL,

    description TEXT,

    priority VARCHAR(20) NOT NULL,

    estimated_labour_hours NUMERIC(5,2),

    mandatory BOOLEAN DEFAULT FALSE,

    display_order INTEGER DEFAULT 1,

    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_inspection_master_item
        FOREIGN KEY (inspection_master_id)
        REFERENCES inspection_master(id)
);


CREATE INDEX idx_inspection_master_vehicle
ON inspection_master
(
    make,
    model,
    fuel_type,
    transmission
);

CREATE INDEX idx_inspection_master_odometer
ON inspection_master
(
    min_odometer,
    max_odometer
);

CREATE INDEX idx_inspection_master_item_master
ON inspection_master_item
(
    inspection_master_id
);


INSERT INTO inspection_master
(
make,
model,
variant,
fuel_type,
transmission,
min_year,
max_year,
min_odometer,
max_odometer
)
VALUES
(
'Honda',
'City',
'VX',
'PETROL',
'MANUAL',
2018,
2022,
60000,
100000
);


INSERT INTO inspection_master_item
(
inspection_master_id,
category,
check_item,
description,
priority,
estimated_labour_hours,
mandatory,
display_order
)
VALUES

(1,'Engine','Engine Oil','Replace engine oil','HIGH',0.50,true,1),

(1,'Engine','Oil Filter','Replace oil filter','HIGH',0.20,true,2),

(1,'Brake','Brake Pads','Inspect brake pads','HIGH',0.80,true,3),

(1,'Brake','Brake Oil','Check brake oil','MEDIUM',0.30,false,4),

(1,'Cooling','Coolant','Inspect coolant','MEDIUM',0.20,false,5),

(1,'Tyres','Wheel Alignment','Check alignment','LOW',0.50,false,6),

(1,'Suspension','Shock Absorbers','Inspect suspension','HIGH',1.20,false,7);



