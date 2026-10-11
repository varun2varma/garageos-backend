# -*- coding: utf-8 -*-
# OEM-neutral workshop SERVICES / LABOUR taxonomy for GarageST (research data, not an official source).
# Line format:  name | aliases | work description and scope | overrides (optional, '&' separated)
# overrides: codes=998714  st=C|NR|U  note=...  unit=JOB|HR  cls=LABOUR_OPERATION|SERVICE_PACKAGE|OUTSOURCED_SERVICE
# Section header: ## category | subcategory | class | unit | default codes | default status | default note
# st: C = CANDIDATE (an official SAC entry names this class of service), NR = NEEDS_REVIEW, U = UNCLASSIFIED.

SERVICES = r"""
## Periodic Maintenance | Fluids & Filters | LABOUR_OPERATION | JOB | 998714 | C | Official SAC 998714 is "Maintenance and repair services of transport machinery and equipment"; confirm it is the correct entry for the garage's supply and whether materials are billed separately.
General Periodic Service | Scheduled Service, Routine Service | Scheduled periodic maintenance work; scope listed on the job card. | st=NR&note=A periodic service usually combines labour with parts: composite or mixed supply treatment is unresolved.
Engine Oil Replacement | Oil Change, Oil Service | Drain and refill engine oil.
Oil Filter Replacement | Oil Filter Change | Replace the engine oil filter.
Air Filter Replacement | Air Filter Change | Replace the engine air filter.
Cabin Filter Replacement | AC Filter Change | Replace the cabin air filter.
Fuel Filter Replacement | Fuel Filter Change | Replace the fuel filter.
Spark Plug Replacement | Plug Change | Replace spark plugs.
Manual Transmission Oil Replacement | Gear Oil Change | Drain and refill gearbox oil.
Automatic Transmission Fluid Replacement | ATF Change | Drain and refill ATF.
Automatic Transmission Fluid Flush | ATF Machine Flush | Machine flush of ATF.
CVT Fluid Replacement | CVT Oil Change | Replace CVT fluid.
Differential Oil Replacement | Axle Oil Change | Replace differential oil.
Transfer Case Oil Replacement | 4WD Oil Change | Replace transfer case oil.
Power Steering Fluid Replacement | PS Fluid Change | Replace power steering fluid.
Coolant Replacement | Coolant Change, Coolant Flush | Drain and refill coolant.
Brake Fluid Replacement | Brake Fluid Change, Brake Bleeding Service | Flush and refill brake fluid.
Clutch Fluid Bleeding | Clutch Bleed | Bleed the hydraulic clutch.
Battery Terminal Cleaning | Terminal Service | Clean and protect battery terminals.
Wiper Blade Replacement | Wiper Change | Replace wiper blades.
Tyre Rotation | Wheel Rotation | Rotate tyres.
## Engine Repair | Engine Mechanical | LABOUR_OPERATION | JOB | 998714 | C | Official SAC 998714: confirm for repair work and the treatment of parts supplied with it.
Drive Belt Replacement | Serpentine Belt Replacement | Replace the accessory drive belt.
Timing Belt Replacement | Cam Belt Replacement | Replace the timing belt and tensioners.
Timing Chain Replacement | Cam Chain Replacement | Replace the timing chain set.
Valve Clearance Adjustment | Tappet Adjustment | Adjust valve clearances.
Cylinder Head Gasket Replacement | Head Gasket Job | Replace the head gasket.
Cylinder Head Overhaul | Head Reconditioning | Remove and overhaul the head.
Engine Top Overhaul | Decarb Job | Top-end overhaul.
Engine Major Overhaul | Complete Engine Overhaul | Full engine overhaul.
Engine Assembly Replacement | Engine Swap | Remove and fit an engine assembly.
Piston Ring Replacement | Ring Job | Replace piston rings.
Crankshaft Oil Seal Replacement | Crank Seal Job | Replace crankshaft seals.
Oil Pan Gasket Replacement | Sump Gasket Job | Replace sump gasket.
Oil Leak Diagnosis and Repair | Leak Repair | Trace and repair oil leaks.
Engine Mount Replacement | Motor Mount Replacement | Replace engine mounts.
Turbocharger Replacement | Turbo Fitting | Remove and fit a turbocharger.
Throttle Body Cleaning | TB Cleaning | Clean the throttle body.
Fuel Injector Cleaning | Injector Ultrasonic Cleaning | Clean injectors.
Fuel Injector Replacement | Injector Change | Replace injectors.
Fuel Pump Replacement | Fuel Pump Change | Replace the fuel pump.
Fuel Tank Cleaning | Tank Flush | Clean the fuel tank.
Carburettor Cleaning and Tuning | Carb Service | Clean and tune a carburettor.
Engine Tune-Up | Tune Up | Engine tune-up.
Engine Decarbonisation | Carbon Clean | Remove carbon deposits.
EGR Valve Cleaning | EGR Clean | Clean the EGR valve.
Exhaust Muffler Repair | Silencer Repair | Repair or weld the muffler.
Exhaust Muffler Replacement | Silencer Replacement | Replace the muffler.
Catalytic Converter Replacement | Cat Replacement | Replace the catalytic converter.
Diesel Particulate Filter Cleaning | DPF Cleaning | Clean the DPF. | cls=OUTSOURCED_SERVICE&st=NR&note=Often performed by a specialist: confirm whether supplied or outsourced.
## Engine Repair | Cooling System | LABOUR_OPERATION | JOB | 998714 | C | Official SAC 998714: confirm.
Water Pump Replacement | Coolant Pump Change | Replace the water pump.
Thermostat Replacement | Thermostat Change | Replace the thermostat.
Radiator Replacement | Radiator Change | Replace the radiator.
Radiator Hose Replacement | Hose Change | Replace radiator hoses.
Radiator Flushing | Cooling System Flush | Flush the cooling system.
Cooling System Pressure Test | Radiator Pressure Test | Pressure test the cooling system.
Cooling Fan Replacement | Radiator Fan Change | Replace the cooling fan.
Radiator Repair | Radiator Rodding, Radiator Soldering | Repair a radiator. | cls=OUTSOURCED_SERVICE&st=NR&note=Often outsourced to a radiator shop: confirm.
## Brakes | Brake Service & Repair | LABOUR_OPERATION | JOB | 998714 | C | Official SAC 998714: confirm.
Brake Inspection | Brake Check | Inspect pads, discs, lines and fluid.
Brake Pad Replacement | Pad Change | Replace brake pads.
Brake Disc Replacement | Rotor Replacement | Replace brake discs.
Brake Disc Resurfacing | Disc Skimming, Rotor Machining | Machine brake discs.
Brake Shoe Replacement | Drum Shoe Change | Replace brake shoes.
Brake Drum Turning | Drum Machining | Machine brake drums.
Brake Caliper Overhaul | Caliper Rebuild | Overhaul a caliper.
Brake Caliper Replacement | Caliper Change | Replace a caliper.
Brake Master Cylinder Replacement | Master Cylinder Change | Replace the master cylinder.
Wheel Cylinder Replacement | Wheel Cylinder Change | Replace wheel cylinders.
Brake Hose Replacement | Brake Flexible Change | Replace brake hoses.
Brake Line Repair | Brake Pipe Repair | Repair brake lines.
Handbrake Adjustment | Parking Brake Adjustment | Adjust the parking brake.
Handbrake Cable Replacement | Parking Brake Cable Change | Replace the parking brake cable.
Brake Booster Replacement | Servo Replacement | Replace the brake booster.
ABS Sensor Replacement | Wheel Speed Sensor Change | Replace an ABS sensor.
ABS Diagnosis | ABS Fault Check | Diagnose ABS faults. | st=NR&note=Diagnostic work may be classified under 998714 or testing services (998346): confirm.
## Clutch & Transmission | Clutch, Gearbox & Drivetrain | LABOUR_OPERATION | JOB | 998714 | C | Official SAC 998714: confirm.
Clutch Inspection | Clutch Check | Inspect the clutch.
Clutch Replacement | Clutch Kit Fitting | Replace the clutch kit.
Clutch Master Cylinder Replacement | Clutch Pump Change | Replace the clutch master cylinder.
Clutch Slave Cylinder Replacement | Clutch Release Cylinder Change | Replace the slave cylinder.
Flywheel Resurfacing | Flywheel Machining | Machine the flywheel. | cls=OUTSOURCED_SERVICE&st=NR
Manual Gearbox Overhaul | Gearbox Repair | Overhaul a manual gearbox.
Automatic Transmission Overhaul | Auto Gearbox Repair | Overhaul an automatic transmission. | cls=OUTSOURCED_SERVICE&st=NR
Gearbox Mount Replacement | Transmission Mount Change | Replace gearbox mounts.
Differential Overhaul | Diff Repair | Overhaul a differential.
Drive Shaft Replacement | CV Axle Replacement | Replace a driveshaft.
CV Joint Replacement | CV Joint Change | Replace a CV joint.
CV Boot Replacement | Axle Boot Change | Replace a CV boot.
Universal Joint Replacement | U-Joint Change | Replace a U-joint.
Propeller Shaft Balancing | Prop Shaft Balancing | Balance a propeller shaft. | cls=OUTSOURCED_SERVICE&st=NR
## Suspension & Steering | Suspension & Steering Repair | LABOUR_OPERATION | JOB | 998714 | C | Official SAC 998714: confirm.
Suspension Inspection | Suspension Check | Inspect suspension.
Shock Absorber Replacement | Damper Replacement | Replace shock absorbers.
Strut Mount Replacement | Top Mount Change | Replace strut mounts.
Control Arm Bush Replacement | Bush Change | Replace control arm bushes.
Ball Joint Replacement | Ball Joint Change | Replace ball joints.
Tie Rod End Replacement | Track Rod End Change | Replace tie rod ends.
Stabiliser Link Replacement | Sway Bar Link Change | Replace stabiliser links.
Coil Spring Replacement | Spring Change | Replace springs.
Wheel Bearing Replacement | Hub Bearing Change | Replace wheel bearings.
Steering Rack Replacement | Rack Change | Replace the steering rack.
Steering Rack Overhaul | Rack Reconditioning | Overhaul the steering rack. | cls=OUTSOURCED_SERVICE&st=NR
Power Steering Pump Replacement | PS Pump Change | Replace the power steering pump.
Steering Wheel Alignment | Steering Centering | Centre the steering wheel.
## Tyres & Wheels | Tyre & Wheel Services | LABOUR_OPERATION | JOB | 998714 | C | Official SAC 998714: confirm that tyre and wheel work is within this entry.
Wheel Alignment | Alignment Service, 4 Wheel Alignment | Measure and adjust alignment angles.
Wheel Balancing | Balancing Service | Balance wheels.
Tyre Fitting | Tyre Change, Tyre Mounting | Remove and fit a tyre.
Tubeless Puncture Repair | Puncture Plug | Repair a tubeless tyre.
Tube-Type Puncture Repair | Tube Patch | Repair a tube-type tyre.
Wheel Alloy Repair | Alloy Straightening | Straighten or repair an alloy wheel. | cls=OUTSOURCED_SERVICE&st=NR
TPMS Service | TPMS Sensor Programming | Service TPMS sensors.
Nitrogen Tyre Filling | N2 Fill | Fill tyres with nitrogen.
Spare Wheel Change | Spare Fitment | Change to the spare wheel.
Wheel Nut Torque Check | Wheel Torque Service | Torque wheel nuts.
## Electrical & Electronics | Electrical Repair | LABOUR_OPERATION | JOB | 998714 | C | Official SAC 998714: confirm.
Electrical Diagnostics | Wiring Fault Finding | Diagnose electrical faults. | st=NR&note=Diagnostic work may be classified under 998714 or testing services (998346): confirm.
Battery Testing | Battery Health Check | Test battery health.
Battery Replacement | Battery Change | Replace the battery.
Battery Charging | Battery Recharge | Charge a battery.
Alternator Replacement | Alternator Change | Replace the alternator.
Alternator Overhaul | Alternator Repair | Overhaul the alternator. | cls=OUTSOURCED_SERVICE&st=NR
Starter Motor Replacement | Starter Change | Replace the starter.
Starter Motor Overhaul | Starter Repair | Overhaul the starter. | cls=OUTSOURCED_SERVICE&st=NR
Wiring Repair | Harness Repair | Repair wiring.
Fuse and Relay Replacement | Fuse Change | Replace fuses and relays.
Headlamp Bulb Replacement | Headlight Bulb Change | Replace headlamp bulbs.
Tail Lamp Bulb Replacement | Tail Light Bulb Change | Replace tail lamp bulbs.
Headlamp Alignment | Headlight Aiming | Aim headlamps.
Headlamp Restoration | Headlight Polishing | Restore headlamp lenses.
Horn Replacement | Horn Change | Replace the horn.
Power Window Repair | Window Regulator Repair | Repair a power window.
Central Locking Repair | Door Lock Repair | Repair central locking.
Instrument Cluster Repair | Dashboard Repair | Repair the cluster. | cls=OUTSOURCED_SERVICE&st=NR
Sensor Replacement | Engine Sensor Change | Replace a sensor. | st=NR&note=Generic name: replacing a sensor spans many systems; split by system or keep as a catch-all (product decision). Confirm 998714 for repair work.
Key Programming | Immobiliser Programming | Program keys. | st=NR
ECU Reprogramming | ECU Flash | Reprogram the ECU. | st=NR&note=Software and programming work may not be repair services: confirm.
ECU Replacement and Coding | ECU Coding | Replace and code an ECU. | st=NR
Airbag System Diagnosis | SRS Diagnosis | Diagnose the SRS. | st=NR
## Diagnostics & Inspection | Diagnostics | LABOUR_OPERATION | JOB | 998714,998346 | NR | Diagnostic and inspection work may fall under repair services (998714) or technical testing and analysis (998346): confirm with a tax professional.
Engine Diagnostics | Engine Scan, Fault Code Scan | Scan and diagnose engine faults.
Computerised Scan | OBD Scan, Full Vehicle Scan | Read and clear fault codes across modules.
Live Data Analysis | Scan Tool Analysis | Analyse live data.
Compression Test | Cylinder Compression Test | Test compression.
Leak Down Test | Cylinder Leak Test | Test cylinder sealing.
Fuel Pressure Test | Fuel System Test | Test fuel pressure.
Smoke Test | EVAP Smoke Test | Smoke test for leaks.
Battery Load Test | Battery Drop Test | Load test a battery.
Charging System Test | Alternator Test | Test charging.
Emission Test | PUC Test | Emission test. | st=U&codes=&note=Statutory emission testing may be a government-authorised activity: no entry selected.
Multi-Point Inspection | MPI, Vehicle Health Check | Structured inspection.
Road Test | Test Drive | Road test. | st=NR
Pre-Purchase Inspection | Used Vehicle Inspection | Inspection before purchase. | codes=998346&st=NR
Underbody Inspection | Chassis Check | Underbody inspection.
Accident Damage Assessment | Damage Estimate Inspection | Assess accident damage. | codes=998346&st=NR
Pre-Trip Safety Check | Highway Trip Check | Pre-trip check.
## Air Conditioning | A/C Service & Repair | LABOUR_OPERATION | JOB | 998714 | C | Official SAC 998714: confirm.
AC Inspection | AC Check | Inspect the A/C system.
AC Servicing | AC Service | Service the A/C system.
AC Gas Refilling | AC Recharge | Evacuate and recharge refrigerant.
AC Leak Test | Leak Detection | Find A/C leaks.
AC Compressor Replacement | Compressor Change | Replace the compressor.
AC Condenser Replacement | Condenser Change | Replace the condenser.
AC Evaporator Replacement | Evaporator Change | Replace the evaporator.
Blower Motor Replacement | Blower Change | Replace the blower.
AC Duct Cleaning | AC Vent Cleaning | Clean ducts. | codes=998538&st=NR
AC Sanitisation | AC Disinfection | Sanitise the A/C. | codes=998538,999719&st=NR
## Body Repair & Paint | Body & Paint | LABOUR_OPERATION | JOB | 998714,998881 | NR | Body repair and painting may be classified as repair services (998714) or as job work on motor vehicles (998881): confirm.
Dent Removal | Paintless Dent Repair, Denting | Remove dents.
Panel Beating | Dent and Straighten | Straighten panels.
Bumper Repair | Bumper Fix | Repair a bumper.
Bumper Replacement | Bumper Fitting | Replace a bumper.
Scratch Repair | Scratch Removal | Repair scratches.
Panel Painting | Panel Respray | Paint a panel.
Full Body Painting | Complete Respray | Paint the vehicle.
Touch-Up Painting | Spot Paint | Touch-up.
Windscreen Replacement | Windshield Change | Replace the windscreen.
Door Glass Replacement | Window Glass Change | Replace door glass.
Rust Treatment | Rust Repair | Treat rust.
Underbody Coating Application | Underseal Application | Apply underbody coating.
Chassis Straightening | Frame Alignment | Straighten the chassis.
Fibre Panel Repair | FRP Repair | Repair fibre panels.
Seat Upholstery Repair | Seat Repair | Repair upholstery. | codes=998729&st=NR
## Cleaning & Detailing | Washing & Detailing | LABOUR_OPERATION | JOB | 998538 | NR | Official SAC 998538 is "Cleaning of transportation equipment"; detailing and coatings may differ: confirm.
Exterior Wash | Car Wash | Exterior wash. | st=C
Interior Vacuum Cleaning | Interior Clean | Vacuum interior. | st=C
Full Wash | Complete Car Wash | Exterior and interior wash. | st=C
Foam Wash | Snow Foam Wash | Foam wash. | st=C
Engine Bay Cleaning | Engine Wash | Engine bay clean. | st=C
Underbody Wash | Chassis Wash | Underbody wash. | st=C
Polishing | Machine Polish | Machine polishing. | st=NR
Waxing | Wax Application | Apply wax.
Ceramic Coating Application | Nano Coating | Apply ceramic coating. | cls=OUTSOURCED_SERVICE&st=NR
Paint Protection Film Installation | PPF Fitting | Install PPF. | cls=OUTSOURCED_SERVICE&st=NR&codes=998714,998739
Interior Deep Cleaning | Interior Detailing | Deep interior clean.
Leather Treatment | Leather Care | Leather treatment.
Odour Removal | Ozone Treatment | Odour removal.
Windscreen Water-Repellent Treatment | Glass Coating | Glass treatment.
## Accessories | Accessory Installation | LABOUR_OPERATION | JOB | 998736,998739,998714 | NR | Installation of accessories may fall under installation services (998736/998739) or repair services (998714): confirm.
Reverse Camera Installation | Camera Fitting | Install a camera.
Parking Sensor Installation | Sensor Fitting | Install parking sensors.
Music System Installation | Stereo Fitting, Car Audio Installation, Head Unit Fitting | Install a stereo.
Dashcam Installation | Dash Camera Fitting, Dash Camera Installation | Install a dashcam.
Fog Lamp Installation | Fog Light Fitting | Install fog lamps.
LED Lighting Installation | LED Upgrade | Install LED lighting.
Seat Cover Fitting | Seat Cover Installation | Fit seat covers.
Floor Mat Fitting | Mat Fitting | Fit floor mats.
Alloy Wheel Fitting | Wheel Upgrade | Fit alloy wheels.
Window Film Installation | Tint Installation | Fit window film.
Central Locking Installation | Remote Lock Fitting | Install central locking.
Alarm and Immobiliser Installation | Security System Fitting | Install a security system.
GPS Tracker Installation | Tracker Fitting | Install a tracker.
Tow Bar Installation | Tow Hitch Fitting | Install a tow bar.
Roof Rail Installation | Roof Rack Fitting, Roof Rack Installation | Install roof rails.
Horn Upgrade Installation | Horn Fitting | Install a horn.
## Electric Vehicle Services | EV Maintenance | LABOUR_OPERATION | JOB | 998714 | NR | EV work: specialist training is required and classification is unresolved; official SAC 998714 is a candidate only.
EV Periodic Inspection | EV Health Check | EV inspection.
High Voltage Battery Health Check | HV Battery Test | HV battery test.
EV Charging Port Inspection | Charge Port Check | Charging port check.
EV Coolant Service | Battery Coolant Change | EV coolant service.
EV Reduction Gear Oil Service | EV Gear Oil Change | EV gear oil change.
EV Brake Service | Regen Brake Service | EV brake service.
EV Software Update and Diagnostics | EV Scan | EV diagnostics.
HV System Isolation Test | Insulation Resistance Test | HV isolation test. | codes=998346,998714
12V Auxiliary Battery Replacement | EV 12V Battery Change | Replace the 12 V battery.
## Roadside & Transport | Towing & Assistance | LABOUR_OPERATION | JOB | 996744 | NR | Official SAC 996744 is "Towing services for commercial and private vehicles"; other roadside services are unresolved.
Vehicle Towing | Breakdown Towing | Tow a vehicle. | st=C
Flatbed Transport | Car Carrier | Flatbed transport. | st=NR&codes=996744,996511
Jump Start | Battery Jump, Battery Jump Start | Jump start.
Roadside Assistance | Breakdown Service | Roadside assistance.
Vehicle Pick-Up and Drop | Pickup Service | Pick-up and drop. | codes=996749&st=NR
## Outsourced Machine Shop | Machining Services | OUTSOURCED_SERVICE | JOB | 998714 | NR | Machine-shop work is commonly performed by a third party; classification depends on whether it is repair or job work: confirm.
Cylinder Boring | Block Boring | Bore cylinders.
Crankshaft Grinding | Crank Regrind | Grind a crankshaft.
Cylinder Head Skimming | Head Resurfacing | Resurface a head.
Valve Seat Cutting | Valve Seat Machining | Cut valve seats.
## Accessories | Audio & Safety Installation | LABOUR_OPERATION | JOB | 998736,998739,998714 | NR | Research-derived addition (coverage audit). Installation versus repair classification is unresolved: confirm.
Fire Extinguisher Fitting | Extinguisher Mounting | Mount vehicle fire extinguisher.
## Tyres & Wheels | Tyre & Wheel Services | LABOUR_OPERATION | JOB | 998714 | NR | Research-derived addition (coverage audit). Confirm that tyre and wheel work is within repair of transport machinery.
TPMS Sensor Replacement | Tyre Pressure Sensor Fitting | Replace tyre pressure monitoring sensor.
Wheel Weight Replacement | Balancing Weight Refit | Replace wheel balancing weights.
## Roadside & Transport | Towing & Assistance | LABOUR_OPERATION | JOB | 996744,996749 | NR | Research-derived addition (coverage audit). Official SAC 996744 names towing services; roadside help may fall in 996749: confirm.
Flat Tyre Roadside Assistance | Puncture Roadside Help | Roadside tyre change or puncture repair.
Fuel Delivery Assistance | Emergency Fuel Supply | Deliver fuel to a stranded vehicle.
## Service Packages | Periodic Packages | SERVICE_PACKAGE | JOB | | NR | A package bundles labour and parts: composite or mixed supply treatment is unresolved and the package is not imported as a catalog service.
Basic Periodic Service Package | Basic Service | Oil, oil filter, fluid levels and basic checks.
Standard Periodic Service Package | Standard Service | Basic package plus air and cabin filter and brake check.
Major Periodic Service Package | Comprehensive Service | Standard package plus plugs, fuel filter and fluids.
Brake Service Package | Brake Care Package | Pads, disc check, fluid and caliper service.
AC Service Package | AC Care Package | A/C inspection, cleaning and gas top-up.
Wheel Care Package | Alignment Balancing Package | Alignment, balancing and rotation.
Pre-Trip Safety Package | Highway Check Package | Tyres, brakes, fluids, battery and lights.
Monsoon Care Package | Pre-Monsoon Package | Wipers, brakes, lights, tyres and battery.
EV Periodic Service Package | EV Care Package | EV inspection and coolant check.
Two-Wheeler Periodic Service Package | Bike Service Package | Oil, filters, chain and brakes for motorcycles.
Commercial Vehicle Periodic Service Package | CV Service Package | Heavy-vehicle periodic checks.
"""
