# -*- coding: utf-8 -*-
# OEM-neutral automotive PARTS taxonomy for GarageST (research data, not an official source).
# Line format:  name | aliases (comma separated) | description | attributes | overrides
# overrides (optional, '&' separated): codes=870830,681381  st=C|NR|U  note=...  unit=NOS  cls=PART|FLUID_LUBRICANT|DETAILING_PRODUCT|WORKSHOP_CONSUMABLE|EV_SPECIALIST
# st: C = CANDIDATE (an official entry names this product class), NR = NEEDS_REVIEW (more than one plausible entry or
# applicability depends on facts not in the catalog), U = UNCLASSIFIED (no supportable official entry found).
# A section header sets the defaults:  ## category | subcategory | system family | class | unit | default codes | default status | default note

PARTS = r"""
## Engine | Cylinder Head & Valve Train | Engine | PART | NOS | 840991,840999 | NR | Parts of engines of 8407/8408: the entry depends on whether the engine is spark-ignition or compression-ignition and on the exact part.
Cylinder Head | Head, Cyl Head | Cast cylinder head carrying valves, ports and combustion chambers. | Engine type: petrol/diesel; Cylinders; Valves per cylinder; Material: aluminium/cast iron
Cylinder Head Gasket | Head Gasket, Gasket Head | Seal between block and head. | Material: MLS/composite; Bore size; Thickness | codes=848410,840991&note=Gasket sets (8484 10) versus engine parts (8409 91): confirm.
Cylinder Head Bolt Set | Head Bolt Kit, Head Stud Kit | Torque-to-yield or stud set for the cylinder head. | Thread size; Quantity; Type: bolt/stud | codes=731815,840991
Valve Cover | Rocker Cover, Tappet Cover | Cover over the valve train. | Material: aluminium/plastic/steel
Valve Cover Gasket | Rocker Cover Gasket, Tappet Cover Gasket | Seal for the valve cover. | Material: rubber/cork/silicone | codes=401693,848410
Intake Valve | Inlet Valve | Engine inlet valve. | Diameter; Stem diameter; Material
Exhaust Valve | Exhaust Poppet Valve | Engine exhaust valve. | Diameter; Stem diameter; Material
Valve Spring | Valve Coil Spring | Spring that closes the valve. | Free length; Wire diameter | codes=732020,840991
Valve Guide | Guide Valve | Guide bushing for the valve stem. | Inner diameter; Length
Valve Stem Seal | Valve Seal | Oil seal on the valve stem. | Stem diameter; Material | codes=401693,848420
Valve Seat Insert | Valve Seat | Hardened seat insert in the head. | Diameter; Material
Tappet | Valve Lifter, Cam Follower | Hydraulic or mechanical valve lifter. | Type: hydraulic/mechanical; Diameter
Rocker Arm | Rocker | Lever transferring cam motion to the valve. | Ratio; Type: roller/slipper
Camshaft | Cam Shaft | Shaft with cam lobes operating the valves. | Engine type; Journals; Material | codes=848310&st=C&note=8483 10 names cam shafts and crank shafts; confirm suitability for the specific engine.
Camshaft Oil Seal | Cam Seal | Rotary seal on the camshaft. | Inner diameter; Material | codes=401693,848420
Camshaft Phaser | VVT Actuator, Cam Phaser | Variable valve timing actuator. | Type: intake/exhaust | codes=840991&st=NR
## Engine | Short Block & Rotating Assembly | Engine | PART | NOS | 840991,840999 | NR | Parts of engines of 8407/8408: spark versus compression ignition and the exact part decide the entry.
Cylinder Block | Engine Block | Block casting housing cylinders and crankcase. | Engine type; Cylinders; Material
Cylinder Liner | Sleeve, Engine Liner | Replaceable cylinder sleeve. | Bore; Type: wet/dry | unit=NOS
Piston | Engine Piston | Piston for one cylinder. | Bore; Oversize; Material
Piston Ring Set | Ring Set, Piston Rings | Compression and oil control rings. | Bore; Oversize; Rings per piston | unit=SET
Piston Pin | Gudgeon Pin, Wrist Pin | Pin joining piston and rod. | Diameter; Length
Connecting Rod | Con Rod | Rod linking piston to crankshaft. | Length; Material
Connecting Rod Bearing Set | Big End Bearing Set, Rod Bearing | Big-end shell bearings. | Size: STD/undersize; Quantity | unit=SET&codes=848330,840991
Crankshaft | Crank Shaft | Engine crankshaft. | Cylinders; Journal size | codes=848310&st=C&note=8483 10 names cam shafts and crank shafts.
Main Bearing Set | Crankshaft Bearing Set | Main journal shell bearings. | Size: STD/undersize | unit=SET&codes=848330,840991
Crankshaft Pulley | Crank Pulley, Harmonic Balancer, Vibration Damper | Pulley or damper on the crank nose. | Groove type; Diameter | codes=848350&st=C
Crankshaft Oil Seal | Crank Seal, Front Seal, Rear Main Seal | Rotary seal on the crankshaft. | Position: front/rear; Inner diameter | codes=401693,848420
Flywheel | Fly Wheel | Mass wheel carrying the starter ring gear. | Type: single mass; Teeth; Diameter | codes=848350&st=C
Dual Mass Flywheel | DMF | Two-mass flywheel damping vibration. | Teeth; Diameter | codes=848350&st=C
Flywheel Ring Gear | Starter Ring Gear | Toothed ring pressed on the flywheel. | Teeth; Diameter | codes=848340,848390
Engine Mounting | Engine Mount, Motor Mount | Rubber or hydraulic engine support. | Position: front/rear/left/right; Type: rubber/hydraulic | codes=870899&note=Residual entry for other motor-vehicle parts: confirm.
Engine Assembly - Petrol | Petrol Engine | Complete petrol engine. | Cylinder capacity band; Cylinders | codes=840731,840732,840733,840734&note=Heading 8407 sub-entries are split by cylinder capacity: confirm the band.
Engine Assembly - Diesel | Diesel Engine | Complete diesel engine for road vehicles. | Cylinders; Capacity | codes=840820&st=C
Engine Gasket Set - Full | Full Gasket Kit, Overhaul Gasket Set | Complete engine gasket kit. | Engine type | unit=SET&codes=848410&st=C
Engine Gasket Set - Top End | Decarb Gasket Set, Top Gasket Kit | Gaskets for cylinder head work. | Engine type | unit=SET&codes=848410&st=C
Oil Pump | Engine Oil Pump | Pump circulating engine oil. | Type: gear/rotor | codes=841330&st=C
Oil Pan | Sump, Oil Sump | Engine oil sump. | Material: steel/aluminium
Oil Pan Gasket | Sump Gasket | Seal for the oil pan. | Material | codes=401693,848410
Oil Drain Plug | Sump Plug | Drain bolt for the sump. | Thread size; Magnetic: yes/no | codes=731815
Oil Drain Plug Washer | Sump Plug Washer, Drain Washer | Sealing washer for the drain plug. | Thread size; Material: copper/aluminium | codes=731822,401693
Oil Filler Cap | Engine Oil Cap | Cap on the oil filler neck. | Thread: screw/bayonet
Oil Dipstick | Engine Dipstick | Dipstick for oil level. | Length
Crankcase Breather Valve | PCV Valve | Positive crankcase ventilation valve. | Type | codes=840991,848180
## Engine | Timing & Drive | Engine | PART | NOS | 840991,840999 | NR | Confirm the entry against the exact component.
Timing Belt | Cam Belt, Toothed Belt | Synchronous rubber belt driving the camshaft. | Teeth; Width; Length | codes=401035,401036&st=NR&note=4010 35 and 4010 36 entries are split by outside circumference: confirm the circumference.
Timing Belt Kit | Cam Belt Kit | Belt with tensioner and idlers. | Includes: belt/tensioner/idler/water pump | unit=KIT&codes=401035,401036,848350
Timing Chain | Cam Chain | Roller chain driving the camshafts. | Pitch; Links | codes=731511,731512
Timing Chain Kit | Chain Kit | Chain with sprockets, guides and tensioner. | Includes: chain/sprockets/guides/tensioner | unit=KIT&codes=731511,848390
Timing Chain Tensioner | Chain Tensioner | Hydraulic or mechanical chain tensioner. | Type | codes=840991
Timing Chain Guide | Chain Guide, Chain Slipper | Plastic guide rail for the chain. | Position | codes=840991
Timing Sprocket | Timing Gear, Cam Gear | Toothed wheel on cam or crank. | Teeth; Position | codes=848390,848340
Timing Belt Tensioner | Cam Belt Tensioner | Spring or hydraulic belt tensioner. | Type | codes=848350
Idler Pulley | Guide Pulley | Fixed pulley guiding a belt. | Diameter; Type: smooth/grooved | codes=848350&st=C
Timing Cover Gasket | Timing Case Gasket | Seal for the timing cover. | Material | codes=401693,848410
Turbocharger | Turbo | Exhaust-driven compressor. | Engine type; Wastegate: yes/no | codes=84148030,841480,840991
Turbocharger Gasket Set | Turbo Fitting Kit | Gaskets and hardware for turbo fitting. | Engine type | unit=KIT&codes=848410
Intercooler | Charge Air Cooler | Air-to-air or air-to-liquid charge cooler. | Type | codes=841950,870891
EGR Valve | Exhaust Gas Recirculation Valve | Valve routing exhaust gas to the intake. | Type: vacuum/electric | codes=848180
Throttle Body | Throttle Valve Assembly | Air throttle assembly. | Type: cable/electronic | codes=840991
Intake Manifold | Inlet Manifold | Air distribution manifold. | Material: plastic/aluminium
Intake Manifold Gasket | Inlet Manifold Gasket | Seal for the intake manifold. | Material | codes=401693,848410
## Lubricants & Fluids | Engine Oils | Lubrication | FLUID_LUBRICANT | LTR | 271019,340319 | NR | Petroleum oil content (70% or more by weight) and the applicable BIS standard decide between 2710 19 and 3403: confirm from the product specification.
Engine Oil - Mineral | Mineral Engine Oil | Petroleum-based engine oil. | Viscosity grade; API/ACEA specification; Pack size
Engine Oil - Semi-Synthetic | Semi Synthetic Engine Oil, Part Synthetic Oil | Blended petroleum and synthetic engine oil. | Viscosity grade; Specification; Pack size
Engine Oil - Fully Synthetic | Synthetic Engine Oil, Full Synthetic Oil | Fully synthetic engine oil. | Viscosity grade; Specification; Pack size
Engine Oil - Diesel Heavy Duty | HD Diesel Oil, CI-4 Oil, Truck Engine Oil | Heavy-duty diesel engine oil. | Viscosity grade; API CK-4/CI-4; Pack size
Engine Oil - Motorcycle 4T | 4T Oil, Bike Engine Oil | Four-stroke motorcycle engine oil. | Viscosity grade; JASO grade; Pack size
Two-Stroke Oil | 2T Oil | Two-stroke engine oil. | Type; Pack size
## Lubricants & Fluids | Transmission & Driveline Fluids | Transmission | FLUID_LUBRICANT | LTR | 271019,340319 | NR | Classification depends on petroleum content and the BIS standard: confirm from the specification.
Manual Transmission Oil | Gear Oil, MTF | Gear oil for manual gearboxes. | Viscosity grade; API GL rating; Pack size | codes=271019&note=Official 8-digit entries exist for automotive gear oil by BIS standard: confirm the standard and petroleum content.
Automatic Transmission Fluid | ATF | Fluid for automatic transmissions. | Specification; Pack size
CVT Fluid | CVT Oil | Fluid for continuously variable transmissions. | Specification; Pack size
Dual Clutch Transmission Fluid | DCT Fluid, DSG Oil | Fluid for dual-clutch gearboxes. | Specification; Pack size
Differential Oil | Hypoid Gear Oil, Axle Oil | Oil for differentials and axles. | Viscosity grade; API GL-5; Pack size
Transfer Case Fluid | 4WD Transfer Oil | Oil for transfer cases. | Specification; Pack size
Power Steering Fluid | PS Fluid, PSF | Hydraulic fluid for power steering. | Type; Pack size | codes=271019,381900&note=Hydraulic transmission liquids may fall under 3819 or 2710: confirm.
Motorcycle Fork Oil | Fork Oil, Suspension Oil | Oil for telescopic forks. | Viscosity; Pack size
## Lubricants & Fluids | Brake & Coolant Fluids | Hydraulics & Cooling | FLUID_LUBRICANT | LTR | 381900 | C | Heading 3819 names hydraulic brake fluids.
Brake Fluid DOT 4 | DOT4 Brake Fluid | Glycol-based brake and clutch fluid. | Grade: DOT 4; Pack size
Brake Fluid DOT 5.1 | DOT5.1 Brake Fluid | Glycol-based high-performance brake fluid. | Grade: DOT 5.1; Pack size
Brake Fluid DOT 3 | DOT3 Brake Fluid | Glycol-based brake fluid. | Grade: DOT 3; Pack size
Coolant Concentrate | Antifreeze Concentrate | Concentrated engine coolant to be diluted. | Chemistry: IAT/OAT/HOAT; Colour; Pack size | codes=382000&st=C&note=Heading 3820 names anti-freezing preparations.
Coolant Ready-Mix | Premixed Coolant, Pre-diluted Coolant | Ready-to-use 50:50 coolant. | Chemistry; Colour; Pack size | codes=382000&st=C&note=Heading 3820 names anti-freezing preparations.
Radiator Flush | Cooling System Cleaner | Chemical cleaner for the cooling circuit. | Pack size | codes=382000,340290&st=NR&unit=LTR
Radiator Stop Leak | Radiator Sealer | Sealant for small cooling leaks. | Pack size | codes=382000,3506&st=NR&unit=LTR
Windscreen Washer Fluid | Washer Concentrate | Screen-wash concentrate or ready fluid. | Type: concentrate/ready; Pack size | codes=340290,382000&st=NR
Diesel Exhaust Fluid | AdBlue, DEF, Urea Solution | Aqueous urea solution for SCR systems. | Concentration: 32.5%; Pack size | codes=310210&st=NR&note=Aqueous urea solution: confirm whether 3102 10 or another entry applies.
## Lubricants & Fluids | Greases & Lubricants | Lubrication | FLUID_LUBRICANT | KG | 340319,271099 | NR | Greases: petroleum content decides between 3403 and 2710.
Multipurpose Grease | Lithium Grease, EP2 Grease | General-purpose chassis and bearing grease. | NLGI grade; Thickener; Pack size
Wheel Bearing Grease | High Temp Bearing Grease | High-temperature wheel bearing grease. | NLGI grade; Pack size
CV Joint Grease | Moly Grease | Molybdenum grease for CV joints. | Pack size
Copper Anti-Seize Paste | Anti Seize, Copper Grease | Anti-seize for exhaust and brake hardware. | Pack size | unit=GM
Silicone Grease | Rubber Grease | Silicone grease for rubber and plastic parts. | Pack size | unit=GM
Brake Caliper Grease | Brake Slide Grease | High-temperature grease for caliper slides. | Pack size | unit=GM
Chain Lubricant | Chain Lube, Chain Spray | Spray lubricant for drive chains. | Type: standard/O-ring; Pack size | unit=ML
Penetrating Oil | Rust Release Spray, Loosening Oil | Rust-penetrating lubricant spray. | Pack size | unit=ML&codes=340319,340399
## Lubricants & Fluids | Additives & Treatments | Fuel & Engine | FLUID_LUBRICANT | ML | 381121,381129,381190 | NR | Heading 3811 covers prepared additives for mineral oils; confirm the exact sub-entry from the composition.
Petrol System Cleaner | Fuel System Cleaner, Injector Cleaner Petrol | Additive cleaning petrol injectors and valves. | Pack size
Diesel System Cleaner | Diesel Injector Cleaner | Additive cleaning diesel injectors. | Pack size
Diesel Anti-Gel Additive | Diesel Conditioner | Additive improving cold flow. | Pack size
Octane Booster | Octane Improver | Additive raising octane rating. | Pack size
Engine Oil Additive | Oil Treatment | Additive for engine oil. | Pack size
Engine Flush | Engine Oil Flush | Cleaner run before an oil change. | Pack size
## Filters | Engine & Fuel Filters | Engine | PART | NOS | 842123 | C | Official entry 8421 23 names oil or petrol filters for internal combustion engines.
Oil Filter - Spin-On | Engine Oil Filter Spin On | Spin-on engine oil filter. | Thread; Height; Diameter; Bypass valve
Oil Filter - Cartridge | Oil Filter Element, Eco Filter | Replaceable oil filter element. | Housing fit; Diameter; Height
Fuel Filter - Petrol | Petrol Filter, Gasoline Filter | Inline or in-tank petrol filter. | Connection; Type: inline/in-tank
Fuel Filter - Diesel | Diesel Filter, Fuel Water Separator Filter | Diesel fuel filter. | Connection; Water separation: yes/no | st=NR&note=Entry 8421 23 names oil or petrol filters; confirm treatment of diesel fuel filters (8421 23 versus 8421 29/8421 39).
Fuel Water Separator | Water Trap, Sedimentor | Separates water from diesel. | Type; Connection | st=NR&codes=842129,842123
Air Filter - Engine Intake | Air Cleaner Element, Engine Air Filter | Engine intake air filter element. | Shape: panel/round; Dimensions | codes=842131&st=C&note=Official entry 8421 31 names intake air filters for internal combustion engines.
Air Filter - Heavy Duty | HD Air Filter, Truck Air Filter | Large intake filter for commercial vehicles. | Dimensions | codes=842131&st=C
Breather Filter | Crankcase Breather Element | Filter on the crankcase breather. | Type | codes=842123,842131&st=NR
Cabin Air Filter - Particulate | Pollen Filter, AC Filter | Passenger compartment air filter. | Dimensions; Type: particulate | codes=842139&st=NR&note=8421 39 is the residual entry for filtering gases: confirm.
Cabin Air Filter - Activated Carbon | Carbon Cabin Filter | Cabin filter with activated carbon layer. | Dimensions | codes=842139&st=NR
Transmission Oil Filter | ATF Filter, Gearbox Filter | Filter for automatic transmission fluid. | Vehicle fit | codes=842123&st=NR
Hydraulic Oil Filter | Power Steering Filter | Filter for hydraulic circuits. | Connection | codes=842123&st=NR
Fuel Pump Strainer | Fuel Sock, In-Tank Strainer | Strainer on the fuel pump inlet. | Type | codes=842123&st=NR
Diesel Particulate Filter | DPF | Particulate filter in the exhaust. | Engine type | codes=842132&st=C&note=Official entry 8421 32 names catalytic converters or particulate filters.
Motorcycle Air Filter | Bike Air Filter | Air filter for motorcycles. | Type: foam/paper; Dimensions | codes=842131&st=C
Motorcycle Oil Filter | Bike Oil Filter | Oil filter for motorcycles. | Type: spin-on/cartridge | codes=842123&st=C
## Brakes | Disc Brake Parts | Braking | PART | SET | 870830,681381 | NR | Mounted pads and brake parts fall under 8708 30; unmounted friction material under 6813 81: confirm whether the item is sold mounted on a backing plate.
Brake Pad Set - Front | Front Brake Pads, Disc Pads Front | Friction pads for front disc brakes. | Axle: front; Material: ceramic/semi-metallic/organic; Wear sensor: yes/no
Brake Pad Set - Rear | Rear Brake Pads, Disc Pads Rear | Friction pads for rear disc brakes. | Axle: rear; Material; Wear sensor
Brake Disc - Front | Front Rotor, Front Brake Rotor | Front brake disc. | Diameter; Type: vented/solid; Pcd | unit=PAIR&codes=870830&st=NR
Brake Disc - Rear | Rear Rotor, Rear Brake Rotor | Rear brake disc. | Diameter; Type: vented/solid | unit=PAIR&codes=870830&st=NR
Brake Caliper | Disc Brake Caliper | Hydraulic disc brake caliper. | Position: front/rear, left/right; Piston count | unit=NOS&codes=870830&st=NR
Brake Caliper Repair Kit | Caliper Seal Kit, Caliper Rebuild Kit | Seals and boots for a caliper. | Piston diameter | unit=KIT&codes=870830,401693
Brake Caliper Guide Pin Kit | Caliper Slider Pin Kit | Slide pins and boots for a caliper. | Type | unit=KIT&codes=870830
Brake Pad Hardware Kit | Anti-Rattle Clip Kit, Shim Kit | Clips and shims for pads. | Axle | unit=KIT&codes=870830
Brake Pad Wear Sensor | Pad Wear Indicator | Electrical wear sensor for pads. | Axle | unit=NOS&codes=870830,853690
## Brakes | Drum Brake Parts | Braking | PART | SET | 870830 | NR | Parts of brakes of motor vehicles: confirm that the item is a brake part and not a separate component.
Brake Shoe Set | Rear Brake Shoes, Drum Shoes | Shoes for drum brakes. | Diameter; Width; Lined/relined
Brake Drum | Rear Brake Drum | Drum for drum brakes. | Diameter; Width | unit=NOS
Wheel Cylinder | Brake Wheel Cylinder, Drum Cylinder | Hydraulic cylinder in drum brakes. | Bore | unit=NOS
Wheel Cylinder Repair Kit | Wheel Cylinder Kit | Seals for a wheel cylinder. | Bore | unit=KIT
Drum Brake Spring Kit | Brake Shoe Spring Kit | Springs and retainers for drum brakes. | Type | unit=KIT
Brake Adjuster | Brake Slack Adjuster | Adjuster for brake clearance. | Type: manual/automatic | unit=NOS
## Brakes | Hydraulic & Control | Braking | PART | NOS | 870830 | NR | Parts of brakes of motor vehicles: confirm exact component.
Brake Master Cylinder | Master Cylinder | Master cylinder of the hydraulic circuit. | Bore; Circuits
Brake Booster | Servo, Vacuum Booster | Vacuum brake servo. | Diameter
Brake Hose - Flexible | Brake Flexible Pipe | Flexible hose between body and caliper. | Length; End fittings | codes=870830,400912&note=Rubber hoses with fittings may fall under 4009: confirm.
Brake Pipe | Brake Line, Hard Line | Rigid steel brake line. | Length; Diameter | codes=870830,7306
Handbrake Cable | Parking Brake Cable | Cable for the parking brake. | Length; Position | codes=870830,731210
Brake Light Switch | Stop Lamp Switch | Switch signalling brake application. | Type | codes=853650&st=NR
Proportioning Valve | Load Sensing Valve | Valve proportioning rear brake pressure. | Type | codes=870830
## Brakes | ABS & Stability | Braking | PART | NOS | 870830 | NR | Electronic brake components: confirm the classification with the tariff entry for electronic control equipment.
Wheel Speed Sensor | ABS Sensor | ABS wheel speed sensor. | Position | codes=902910,903180,870830
ABS Hydraulic Modulator | ABS Pump, ABS Unit | Hydraulic unit of the ABS. | Vehicle fit | codes=870830
ABS Control Module | ABS ECU | Electronic module controlling ABS. | Vehicle fit | codes=853710,870830
## Clutch & Transmission | Clutch | Transmission | PART | NOS | 870893 | NR | Official entry 8708 93 names clutches and parts for the motor vehicles of 8701 to 8705; confirm for components sold separately.
Clutch Kit | Clutch Set | Clutch disc, pressure plate and release bearing. | Diameter; Splines; Includes | unit=KIT&st=C
Clutch Disc | Clutch Plate, Friction Plate | Driven clutch disc. | Diameter; Splines
Clutch Pressure Plate | Clutch Cover | Pressure plate assembly. | Diameter
Clutch Release Bearing | Throw-out Bearing | Release bearing. | Type: mechanical/hydraulic | codes=870893,848210&st=NR
Clutch Master Cylinder | Clutch Pump | Hydraulic clutch master cylinder. | Bore
Clutch Slave Cylinder | Clutch Release Cylinder | Hydraulic clutch slave cylinder. | Bore
Clutch Cable | Clutch Wire | Clutch actuation cable. | Length | codes=870893,731210
Clutch Fork | Release Fork | Fork operating the release bearing. | Vehicle fit
## Clutch & Transmission | Gearbox & Drivetrain | Transmission | PART | NOS | 870840,870850,848340 | NR | Gearbox and axle parts: official 8708 entries cover gear boxes and drive-axles for motor vehicles; confirm the part.
Gearbox - Manual | Manual Transmission Assembly | Complete manual gearbox. | Speeds; Drive | codes=870840&st=C
Gearbox - Automatic | Automatic Transmission Assembly | Complete automatic gearbox. | Type: torque converter/CVT/DCT | codes=870840&st=C
Gearbox Mounting | Transmission Mount | Support for the gearbox. | Position | codes=870899
Gear Shift Cable | Gear Linkage Cable | Shift cable. | Length | codes=870840,731210
Gear Set | Gear Cluster, Synchro Gear | Gear and synchroniser parts. | Gear position | codes=848340,870840
Torque Converter | TC | Fluid coupling for automatic gearboxes. | Vehicle fit | codes=848340,870840
Differential Assembly | Diff Assembly, Final Drive | Differential unit. | Ratio | codes=870850&st=C
Crown Wheel and Pinion | Ring Gear and Pinion | Final drive gear set. | Ratio | unit=SET
Differential Bearing | Diff Bearing | Bearing in the differential. | Type | codes=848220,848210
Drive Shaft Assembly | CV Axle, Half Shaft | Driveshaft with joints. | Position: front/rear, left/right | codes=870850,870899
Propeller Shaft | Prop Shaft, Cardan Shaft | Rear-drive propeller shaft. | Length; Joints | codes=848310,870899
CV Joint | Constant Velocity Joint | Outer or inner CV joint. | Position: inner/outer | codes=848360,870850
CV Boot Kit | Axle Boot Kit | Boot, clamps and grease for CV joint. | Position: inner/outer | unit=KIT&codes=401693,870850
Universal Joint | U-Joint, Cross Joint | Cardan cross joint. | Size | codes=848360&st=C
Centre Support Bearing | Prop Shaft Bearing | Centre bearing of the propeller shaft. | Size | codes=848210,870899
## Suspension & Steering | Suspension | Suspension | PART | NOS | 870880 | NR | Official entry 8708 80 names suspension systems and parts including shock absorbers; confirm the part.
Shock Absorber - Front | Front Damper | Front hydraulic or gas damper. | Type: oil/gas; Position | codes=870880&st=C
Shock Absorber - Rear | Rear Damper | Rear hydraulic or gas damper. | Type: oil/gas | codes=870880&st=C
Strut Assembly | Complete Strut, Coil-over Strut | Strut with spring and mount. | Position
Strut Mount | Top Mount, Strut Top | Upper strut mounting. | Position
Strut Bearing | Top Mount Bearing | Bearing in the strut mount. | Size
Coil Spring | Suspension Spring | Coil suspension spring. | Position; Rate; Free length | codes=732020,870880
Leaf Spring | Spring Leaf, Leaf Pack | Leaf spring or leaf. | Leaves; Length | codes=732010,870880
Spring Bump Stop | Bump Rubber, Dust Cover Kit | Bump stop and dust boot. | Position | unit=KIT
Control Arm | Wishbone, Lower Arm | Suspension control arm. | Position: lower/upper, left/right
Ball Joint | Suspension Ball Joint | Ball joint of the control arm. | Position
Suspension Bush | Control Arm Bush, Rubber Bush | Rubber or polyurethane bush. | Position; Size
Stabiliser Bar | Anti-Roll Bar, Sway Bar | Anti-roll bar. | Position
Stabiliser Link | Sway Bar Link, Drop Link | Link for the anti-roll bar. | Position
Stabiliser Bush | Sway Bar Bush | Bush for the anti-roll bar. | Size
Trailing Arm | Rear Arm | Rear suspension arm. | Position
Air Spring | Air Suspension Bellow | Air suspension spring. | Position | codes=870880
## Suspension & Steering | Steering | Steering | PART | NOS | 870894 | NR | Official entry 8708 94 names steering wheels, columns and boxes and parts; confirm the part.
Steering Rack | Rack and Pinion | Steering rack assembly. | Type: manual/hydraulic/electric | st=C
Steering Rack Boot | Rack Gaiter | Boot for the steering rack. | Position | codes=870894,401693
Steering Wheel | Steering Wheel Assembly | Steering wheel. | Diameter; Airbag: yes/no | st=C
Steering Column | Steering Shaft | Steering column. | Vehicle fit | st=C
Steering Gear Box | Steering Box | Recirculating-ball steering box. | Type | st=C
Tie Rod End | Outer Tie Rod, Track Rod End | Outer steering joint. | Position
Inner Tie Rod | Rack End, Inner Track Rod | Inner steering joint. | Position
Power Steering Pump | PS Pump | Hydraulic power steering pump. | Type | codes=841360,870894
Power Steering Hose | PS Hose, High Pressure Hose | Power steering hose. | Type: pressure/return | codes=400912,870894
Steering Idler Arm | Idler Arm | Steering linkage arm. | Vehicle fit
Drag Link | Centre Link | Steering linkage rod. | Length
King Pin Kit | Steering Knuckle Pin Kit | Kingpin and bush set. | Vehicle fit | unit=KIT
## Suspension & Steering | Wheel Bearings & Hubs | Wheels | PART | NOS | 848210,848220 | NR | Bearing type determines the entry (ball, tapered roller, other): confirm.
Wheel Bearing - Front | Front Hub Bearing | Front wheel bearing. | Type: ball/tapered; Size
Wheel Bearing - Rear | Rear Hub Bearing | Rear wheel bearing. | Type; Size
Wheel Hub Assembly | Hub Unit, Hub Bearing Unit | Hub with integrated bearing. | Position; ABS ring | codes=870870,870899
Wheel Stud | Wheel Bolt | Stud or bolt holding the wheel. | Thread; Length | codes=731815
Wheel Nut | Lug Nut | Nut holding the wheel. | Thread; Seat type | codes=731816
Wheel Bearing Seal | Hub Seal | Seal for the wheel bearing. | Size | codes=401693,848420
## Cooling System | Cooling | Cooling | PART | NOS | 870891 | NR | Radiators and parts: official entry 8708 91; confirm for components sold separately.
Radiator | Engine Radiator | Engine coolant radiator. | Core size; Material: aluminium/copper; Transmission cooler: yes/no | st=C
Radiator Cap | Pressure Cap | Pressure cap of the cooling system. | Pressure rating | codes=870891
Radiator Hose - Upper | Top Hose | Upper radiator hose. | Length | codes=400911,400912,870891
Radiator Hose - Lower | Bottom Hose | Lower radiator hose. | Length | codes=400911,400912,870891
Heater Hose | Heater Pipe | Heater circuit hose. | Length | codes=400911,400912
Coolant Expansion Tank | Overflow Tank, Reservoir | Coolant reservoir. | Cap: yes/no | codes=870891
Water Pump | Coolant Pump | Coolant circulation pump. | Drive: belt/chain/electric | codes=841330&st=C&note=Official entry 8413 30 names fuel, lubricating or cooling medium pumps for internal combustion piston engines.
Thermostat | Coolant Thermostat | Thermostat regulating coolant temperature. | Opening temperature | codes=903210
Thermostat Housing | Water Outlet | Housing of the thermostat. | Material | codes=840991
Radiator Fan | Cooling Fan | Radiator cooling fan. | Diameter; Type: electric/belt | codes=841459,841451
Radiator Fan Motor | Fan Motor | Motor of the radiator fan. | Voltage | codes=850110,850120
Fan Clutch | Viscous Fan Clutch | Clutch of a belt-driven fan. | Type | codes=841490
Radiator Fan Shroud | Fan Cowl | Shroud around the fan. | Vehicle fit | codes=870891
Engine Oil Cooler | Oil Cooler Assembly | Oil-to-coolant cooler. | Type | codes=841950
Heater Core | Heater Matrix | Cabin heater core. | Size | codes=841950,870891
Coolant Temperature Sensor | ECT Sensor, Water Temp Sensor | Sensor for coolant temperature. | Type | codes=903289,902519
## Fuel System | Fuel Delivery | Fuel | PART | NOS | 841330 | NR | Official entry 8413 30 names fuel pumps for internal combustion piston engines; confirm per component.
Fuel Pump - Electric | In-Tank Fuel Pump, Petrol Pump | Electric fuel pump. | Type; Pressure | st=NR&note=8413 30 names pumps for internal combustion engines; an in-tank electric pump may fall elsewhere: confirm
Fuel Pump Module | Fuel Pump Assembly | Pump with sender and housing. | Vehicle fit | st=NR&note=Assembly of pump, sender and housing: confirm the classification of the assembly
Fuel Pump - Mechanical | Mechanical Petrol Pump | Camshaft-driven fuel pump. | Type | st=C
Diesel Injection Pump | Fuel Injection Pump, Inline Pump | Diesel injection pump. | Type: inline/rotary | st=C
High Pressure Fuel Pump | Common Rail Pump, GDI Pump | High-pressure pump for direct injection. | Type
Fuel Injector - Petrol | Petrol Injector | Petrol fuel injector. | Type: port/direct | codes=840991&st=NR
Fuel Injector - Diesel | Diesel Injector, Common Rail Injector | Diesel fuel injector. | Type | codes=840999,848180&st=NR
Injector Seal Kit | Injector O-Ring Kit | Seals for injectors. | Engine type | unit=KIT&codes=401693
Fuel Rail | Injector Rail | Fuel distribution rail. | Engine type | codes=840991
Fuel Pressure Regulator | Pressure Regulator | Regulates fuel pressure. | Type | codes=848180,840991
Fuel Tank | Petrol Tank, Diesel Tank | Fuel tank. | Capacity; Material: steel/plastic | codes=870899
Fuel Tank Cap | Fuel Cap | Fuel filler cap. | Type: locking/non-locking | codes=870899
Fuel Hose | Fuel Line, Petrol Pipe | Fuel hose. | Length; Diameter | codes=400911,400912
Carburettor | Carb | Carburettor. | Throat size; Vehicle fit | codes=840991&st=NR
Carburettor Repair Kit | Jet Kit, Carb Kit | Jets, gaskets and needles. | Vehicle fit | unit=KIT&codes=840991
Fuel Level Sender | Fuel Gauge Sender | Fuel level sensor. | Type | codes=902610&st=NR
## Electrical | Battery & Charging | Electrical | PART | NOS | 850710 | NR | Official entry 8507 10 names lead-acid batteries of a kind used for starting piston engines.
Car Battery - Lead Acid | Starting Battery, SMF Battery | 12 V lead-acid battery for cars. | Capacity Ah; CCA; Terminal layout; Type: flooded/SMF | st=C
Heavy Duty Battery | Truck Battery, Bus Battery | Lead-acid battery for commercial vehicles. | Capacity Ah; Voltage | st=C
Motorcycle Battery | Two-Wheeler Battery | Battery for motorcycles. | Capacity Ah; Type: dry/gel/lithium | codes=850710,850720,850760&st=NR
AGM Start-Stop Battery | EFB Battery | Battery for start-stop systems. | Capacity Ah; Technology: AGM/EFB | codes=850710&st=NR
Battery Terminal | Battery Clamp | Terminal clamp. | Polarity | codes=853690
Battery Cable | Battery Lead | Battery cable. | Length; Gauge | codes=854430,854442
Alternator | Generator | Charging alternator. | Output A; Pulley type | codes=851150&st=C&note=Official entry 8511 50 names other generators for internal combustion engines.
Alternator Regulator | Voltage Regulator | Alternator voltage regulator. | Voltage | codes=851190
Alternator Rectifier | Rectifier Bridge | Diode bridge of the alternator. | Voltage | codes=851190
Alternator Pulley | Alternator Freewheel Pulley | Alternator drive pulley. | Type | codes=848350
## Electrical | Starting & Ignition | Electrical | PART | NOS | 851190 | NR | Parts of 8511 equipment: confirm the exact sub-entry.
Starter Motor | Self Starter, Cranking Motor | Engine starter motor. | Voltage; Power | codes=851140&st=C
Starter Solenoid | Starter Relay | Solenoid of the starter. | Voltage
Starter Drive | Bendix Drive | Pinion drive of the starter. | Teeth
Ignition Coil | Coil Pack | Ignition coil. | Type: single/pack | codes=851130&st=C
Spark Plug - Iridium | Iridium Plug | Iridium electrode spark plug. | Thread; Reach; Heat range | codes=851110&st=C&unit=NOS
Spark Plug - Platinum | Platinum Plug | Platinum electrode spark plug. | Thread; Reach; Heat range | codes=851110&st=C
Spark Plug - Standard | Copper Plug, Nickel Plug | Standard spark plug. | Thread; Reach; Heat range | codes=851110&st=C
Glow Plug | Diesel Glow Plug | Diesel pre-heating plug. | Voltage; Length | codes=851180&st=NR
HT Lead Set | Spark Plug Wires, Ignition Leads | High-tension lead set. | Cylinders | unit=SET&codes=854430&st=NR
Distributor Cap | Distributor Cover | Distributor cap. | Cylinders | codes=851190
Ignition Switch | Key Switch | Ignition switch. | Type | codes=853650
Magneto Coil | Stator Coil, Magneto | Magneto or stator coil. | Vehicle fit | codes=851120&st=NR
## Electrical | Wiring, Switches & Relays | Electrical | PART | NOS | 853650 | NR | Switchgear classification depends on type and voltage: confirm.
Fuse - Blade | Automotive Fuse, ATO Fuse | Blade-type fuse. | Rating A; Type | codes=853610&st=C&unit=PKT
Fuse - Cartridge | Glass Fuse | Cartridge fuse. | Rating A | codes=853610&st=C&unit=PKT
Relay - Automotive | Relay 12V, ISO Relay | Automotive relay. | Voltage; Contacts | codes=853641&st=C
Flasher Relay | Indicator Relay | Indicator flasher. | Voltage | codes=853641
Wiring Harness | Wire Harness, Engine Harness | Vehicle wiring harness. | Section | codes=85443000&st=C&note=Official entry 8544 30 names ignition wiring sets and other wiring sets of a kind used in vehicles.
Fuse Box | Fuse Block, Junction Box | Fuse and relay box. | Vehicle fit | codes=853690,853710
Combination Switch | Indicator Wiper Stalk | Steering column switch. | Functions
Headlamp Switch | Light Switch | Headlamp switch. | Type
Hazard Switch | Hazard Warning Switch | Hazard switch. | Type
Power Window Switch | Window Switch | Window switch. | Position
Reverse Lamp Switch | Reverse Switch | Reverse light switch. | Type
Horn | Car Horn | Electric horn. | Voltage; Tone | codes=851230&st=C
Horn Relay | Horn Switch Relay | Relay for the horn. | Voltage | codes=853641
Power Window Motor | Window Regulator Motor | Window motor. | Position | codes=850110,851290
Window Regulator | Window Lifter | Window regulator. | Position; Type | codes=870899
Central Locking Actuator | Door Lock Actuator | Central locking actuator. | Position | codes=830120,850110
Wiper Motor | Windscreen Wiper Motor | Windscreen wiper motor. | Position: front/rear | codes=851240,851290&st=NR
Blower Motor | Heater Blower | Cabin blower motor. | Voltage | codes=841451,841459,850110
Blower Resistor | Blower Speed Resistor | Blower speed control resistor. | Vehicle fit | codes=853340
## Sensors & Control Modules | Engine Sensors | Electronics | PART | NOS | 902910,903180 | NR | Sensor classification depends on the measured quantity and function; no single official entry is assumed.
Crankshaft Position Sensor | CKP Sensor | Crankshaft position sensor. | Type: Hall/inductive
Camshaft Position Sensor | CMP Sensor | Camshaft position sensor. | Type
Oxygen Sensor | Lambda Sensor, O2 Sensor | Exhaust oxygen sensor. | Wires; Position: pre/post cat
NOx Sensor | Nitrogen Oxide Sensor | Exhaust NOx sensor. | Position
MAP Sensor | Manifold Pressure Sensor | Manifold pressure sensor. | Type
MAF Sensor | Air Flow Meter | Mass air flow sensor. | Type
Throttle Position Sensor | TPS | Throttle position sensor. | Type
Knock Sensor | Detonation Sensor | Knock sensor. | Thread
Intake Air Temperature Sensor | IAT Sensor | Intake air temperature sensor. | Type
Oil Pressure Switch | Oil Pressure Sensor | Oil pressure sensor or switch. | Thread
Fuel Rail Pressure Sensor | Rail Pressure Sensor | Fuel rail pressure sensor. | Type
## Sensors & Control Modules | Chassis & Body Sensors | Electronics | PART | NOS | 902910,903180 | NR | Sensor classification depends on the measured quantity and function.
Parking Sensor | Reverse Sensor, PDC Sensor | Ultrasonic parking sensor. | Colour; Position
Tyre Pressure Monitoring Sensor | TPMS Sensor | Tyre pressure sensor. | Frequency
Steering Angle Sensor | SAS | Steering angle sensor. | Type
Rain Sensor | Wiper Rain Sensor | Rain sensor. | Type
Airbag Control Module | SRS ECU | Airbag control unit. | Vehicle fit | codes=853710,870895
Airbag Clock Spring | Spiral Cable | Steering airbag contact. | Vehicle fit | codes=870895
## Sensors & Control Modules | Control Modules | Electronics | PART | NOS | 853710 | NR | Electronic control units: confirm 8537 10 versus other entries.
Engine Control Module | ECU, ECM | Engine control unit. | Vehicle fit; Programmed: yes/no
Body Control Module | BCM | Body control unit. | Vehicle fit
Transmission Control Module | TCM | Transmission control unit. | Vehicle fit
Immobiliser Module | Immobilizer Unit | Immobiliser unit. | Vehicle fit
Instrument Cluster | Dashboard Cluster | Instrument cluster. | Vehicle fit | codes=902910,902920
Speedometer | Speed Indicator | Speed indicator. | Type: cable/electronic | codes=902920&st=C
Tachometer | Rev Counter | Engine speed indicator. | Type | codes=902920&st=C
Speedometer Cable | Speedo Cable | Speedometer drive cable. | Length | codes=902990,731210
## Lighting & Visibility | Lamps | Lighting | PART | NOS | 851220 | NR | Official entry 8512 20 names other lighting or visual signalling equipment; confirm it covers the specific lamp.
Headlamp Assembly | Headlight, Head Lamp | Complete headlamp. | Position: left/right; Technology: halogen/LED | st=C
Tail Lamp Assembly | Tail Light, Rear Lamp | Complete tail lamp. | Position; Technology | st=C
Fog Lamp Assembly | Fog Light | Fog lamp. | Position | st=C
Indicator Lamp Assembly | Turn Signal Lamp | Indicator lamp. | Position | st=C
Side Marker Lamp | Parking Lamp | Side marker. | Position
Number Plate Lamp | License Lamp | Number plate lamp. | Type
Interior Lamp | Dome Lamp, Roof Lamp | Interior lamp. | Type
Halogen Bulb | H4 H7 Bulb, Headlamp Halogen | Halogen headlamp bulb. | Type: H1/H4/H7/H11; Wattage | codes=853921&st=NR&note=Vehicle lamp coverage of 8539 is uncertain (heading text anomaly in the source file); confirm the entry
LED Bulb | LED Headlamp Bulb, LED Retrofit | LED lamp for vehicles. | Type; Wattage | codes=853952&st=NR&note=Vehicle lamp coverage of 8539 is uncertain (heading text anomaly in the source file); confirm the entry
HID Xenon Bulb | Xenon Bulb, HID Lamp | Discharge headlamp bulb. | Type: D1S/D2S/D3S | codes=853932,853939&st=NR
Signal Bulb | Indicator Bulb, Brake Bulb | Small signal bulb. | Type: P21W/PY21W/W5W; Wattage | codes=853922,853929&st=NR
Headlamp Ballast | HID Ballast | Ballast of the HID system. | Voltage | codes=850410
Headlamp Levelling Motor | Headlamp Adjuster | Headlamp levelling actuator. | Position | codes=851290
## Lighting & Visibility | Wipers, Washers & Mirrors | Visibility | PART | NOS | 851240,851290 | NR | Official entry 8512 40 names windscreen wipers; blades and arms may be classified as parts (8512 90): confirm.
Wiper Blade - Front | Windscreen Wiper, Wiper Rubber | Front wiper blade. | Length; Type: conventional/flat | unit=PAIR
Wiper Blade - Rear | Rear Wiper | Rear wiper blade. | Length
Wiper Arm | Wiper Lever | Wiper arm. | Position
Wiper Linkage | Wiper Mechanism | Wiper linkage. | Vehicle fit
Washer Pump | Windscreen Washer Pump | Washer pump. | Voltage | codes=841370,841381
Washer Reservoir | Washer Bottle | Washer fluid tank. | Capacity
Washer Nozzle | Windshield Washer Jet, Washer Jet | Windscreen washer spray nozzle. | Type: fixed/heated | codes=851240,851290
Exterior Mirror Assembly | Side Mirror, Wing Mirror | Complete exterior mirror. | Position; Electric: yes/no | codes=700910,870829&note=Entry 7009 10 names rear-view mirrors for vehicles.
Mirror Glass | Mirror Plate | Mirror glass only. | Position | codes=700910&st=C
Interior Mirror | Rear View Mirror Inside | Interior mirror. | Type | codes=700910&st=C
Mirror Cover | Mirror Cap | Mirror cover. | Position | codes=870829
## Air Conditioning | A/C Components | Cabin Climate | PART | NOS | 841590 | NR | Parts of air conditioning machines: confirm the specific entry (8415 20 for motor-vehicle machines, 8414 30 for compressors).
AC Compressor | A/C Compressor | Refrigerant compressor. | Type: fixed/variable; Clutch; Vehicle fit | codes=841430&st=C&note=Official entry 8414 30 names compressors of a kind used in refrigerating equipment.
AC Compressor Clutch | Compressor Clutch Coil | Electromagnetic clutch. | Vehicle fit | codes=841490,850520
AC Condenser | Condenser Radiator | A/C condenser. | Core size | codes=841590,841950
AC Evaporator | Evaporator Coil | A/C evaporator. | Vehicle fit | codes=841590,841950
AC Expansion Valve | TXV | Thermal expansion valve. | Type | codes=848180
AC Receiver Drier | Accumulator Drier | Drier or accumulator. | Type | codes=841590
AC Hose | AC Pipe | Refrigerant hose. | Position | codes=400912,841590
AC Pressure Switch | Binary Switch | A/C pressure switch. | Type | codes=903289,853650
AC Control Panel | Climate Control Unit | A/C control. | Vehicle fit | codes=853710
AC O-Ring Kit | AC Seal Kit | Seals for A/C joints. | Type | unit=KIT&codes=401693
Automotive AC Unit | Vehicle Air Conditioner | Complete A/C system for a vehicle. | Type | codes=841520&st=C
## Lubricants & Fluids | A/C Refrigerants & Oils | Cabin Climate | FLUID_LUBRICANT | KG | 290339 | NR | Refrigerants are classified by chemical composition (single compound versus blend): confirm from the product data.
Refrigerant R134a | HFC-134a, AC Gas 134a | Refrigerant R134a. | Pack size
Refrigerant R1234yf | HFO-1234yf, AC Gas 1234yf | Refrigerant R1234yf. | Pack size
AC Compressor Oil | PAG Oil, POE Oil | A/C compressor lubricant. | Type: PAG/POE; Pack size | unit=ML&codes=340319,271019
AC Leak Detection Dye | UV Dye | UV dye for leak detection. | Pack size | unit=ML&codes=3204
## Tyres & Wheels | Tyres & Tubes | Wheels | PART | NOS | 401110 | NR | Official heading 4011 splits new pneumatic tyres by vehicle kind.
Car Tyre | Passenger Tyre, Radial Tyre | New pneumatic tyre for motor cars. | Size; Load index; Speed rating; Tubeless | codes=401110&st=C
Truck and Bus Tyre | Lorry Tyre, HCV Tyre | New pneumatic tyre for buses or lorries. | Size; Ply rating | codes=401120&st=C
Motorcycle Tyre | Two-Wheeler Tyre, Scooter Tyre | New pneumatic tyre for motorcycles. | Size; Position | codes=401140&st=C
Agricultural Tyre | Tractor Tyre | Tyre for agricultural vehicles. | Size | codes=401170&st=C
Retreaded Tyre | Remould Tyre | Retreaded pneumatic tyre. | Size | codes=401211,401212&st=NR
Inner Tube - Car | Tube Car | Inner tube for cars. | Size; Valve | codes=401310&st=C
Inner Tube - Motorcycle | Tube Two Wheeler | Inner tube for motorcycles. | Size | codes=401390&st=NR
Tyre Flap | Rim Flap | Flap for tube-type tyres. | Size | codes=401290&st=NR
Tyre Valve | Valve Stem | Tyre valve. | Type: snap-in/clamp-in | codes=848180&st=NR
## Tyres & Wheels | Wheels & Balancing | Wheels | PART | NOS | 870870 | NR | Official entry 8708 70 names road wheels and parts and accessories; confirm per item.
Alloy Wheel | Alloy Rim, Mag Wheel | Alloy road wheel. | Diameter; Width; PCD; Offset | st=C
Steel Wheel | Steel Rim | Steel road wheel. | Diameter; Width; PCD | st=C
Wheel Cap | Hub Cap, Centre Cap | Wheel centre cap. | Diameter
Wheel Cover | Wheel Trim | Wheel cover. | Diameter
Wheel Balancing Weight - Clip-On | Balancing Weight Clip | Clip-on balancing weight. | Mass g | unit=PKT&codes=&st=U&note=Weights are articles of lead, zinc or steel: no supported official entry identified; confirm material and entry.
Wheel Balancing Weight - Adhesive | Stick-On Weight | Adhesive balancing weight. | Mass g | unit=ROLL&codes=&st=U&note=Weights are articles of lead, zinc or steel: no supported official entry identified.
Tyre Sealant | Puncture Sealant | Puncture sealing liquid. | Pack size | unit=ML&codes=3506&st=NR
Tyre Repair Plug | Tubeless Repair Strip | Rubber repair plug. | Type | unit=PKT&codes=4006&st=NR
Tyre Bead Lubricant | Tyre Mounting Paste | Fitting paste. | Pack size | unit=KG&codes=340319&st=NR
## Body & Exterior | Panels & Glass | Body | PART | NOS | 870810,870829 | NR | Body parts: official entries 8708 10 (bumpers), 8708 29 (other body parts) and 8707 (bodies) apply to motor vehicles of 8701 to 8705; confirm.
Front Bumper | Front Bumper Cover | Front bumper. | Position; Painted: yes/no | codes=870810&st=C
Rear Bumper | Rear Bumper Cover | Rear bumper. | Painted | codes=870810&st=C
Bumper Reinforcement | Bumper Beam | Bumper beam. | Position | codes=870810
Bumper Grille | Lower Grille | Bumper grille. | Position | codes=870829
Radiator Grille | Front Grille | Front grille. | Vehicle fit | codes=870829
Front Fender | Wing Panel | Front fender. | Position | codes=870829
Bonnet | Hood | Bonnet. | Vehicle fit | codes=870829
Boot Lid | Tailgate, Trunk Lid | Boot lid. | Vehicle fit | codes=870829
Door Shell | Door Panel | Door shell. | Position | codes=870829
Windscreen | Front Windshield | Laminated windscreen. | Features: tint/rain sensor | codes=870822,700711&st=NR&note=Official entry 8708 22 names windscreens; 7007 11 names safety glass sized for vehicles.
Rear Windscreen | Back Glass | Rear window glass. | Heated: yes/no | codes=870822,700711&st=NR
Door Glass | Side Window Glass | Door window glass. | Position | codes=870822,700711
Bonnet Gas Strut | Bonnet Stay | Gas strut. | Vehicle fit | codes=870829
Mud Flap | Mudguard | Mud flap. | Position | codes=870829
Wheel Arch Liner | Fender Liner | Wheel arch liner. | Position | codes=870829
Underbody Shield | Engine Undercover | Underbody shield. | Vehicle fit | codes=870829
Spoiler | Rear Spoiler | Spoiler. | Type | codes=870829
Vehicle Emblem | Badge | Emblem. | Type | codes=870829
## Body & Exterior | Door Hardware & Seals | Body | PART | NOS | 830230 | NR | Official entry 8302 30 names mountings and fittings suitable for motor vehicles; locks are 8301 20.
Door Handle - Exterior | Outer Door Handle | Exterior handle. | Position | st=C
Door Handle - Interior | Inner Door Handle | Interior handle. | Position | st=C
Door Hinge | Door Hinge Set | Hinge. | Position | st=C
Door Lock Assembly | Door Latch | Door lock. | Position | codes=830120&st=C&note=Official entry 8301 20 names locks of a kind used for motor vehicles.
Door Lock Cylinder | Key Barrel | Lock cylinder. | Position | codes=830120&st=C
Door Check Strap | Door Stopper | Door stay. | Position | st=C
Bonnet Latch | Hood Latch | Bonnet latch. | Type | st=C
Door Weatherstrip | Door Seal, Rubber Beading | Door sealing rubber. | Position | codes=401699,870829&st=NR
Window Channel Seal | Glass Run Channel | Window channel rubber. | Position | codes=401699,870829&st=NR
## Interior & Safety | Seats & Restraints | Interior | PART | NOS | 870821,940120 | NR | Official entries 8708 21 (safety seat belts) and 9401 20 (seats for motor vehicles).
Seat Belt | Safety Belt | Seat belt. | Position | codes=870821&st=C
Seat Belt Buckle | Belt Buckle | Seat belt buckle. | Position | codes=870821&st=C
Seat Assembly | Car Seat | Vehicle seat. | Position | codes=940120&st=C
Seat Cover Set | Seat Covers | Seat covers. | Material | unit=SET&codes=630492&st=NR
Floor Mat Set - Rubber | Rubber Mats | Rubber floor mats. | Vehicle fit | unit=SET&codes=401691&st=C&note=Official entry 4016 91 names rubber floor coverings and mats.
Floor Mat Set - Carpet | Carpet Mats | Carpet floor mats. | Vehicle fit | unit=SET&codes=570390&st=NR
Airbag Module | Driver Airbag | Airbag with inflator. | Position | codes=870895&st=C
Sun Visor | Sunvisor | Sun visor. | Position | codes=870899
Dashboard Panel | Dash Board | Dashboard. | Vehicle fit | codes=870899
## Exhaust & Emission | Exhaust System | Exhaust | PART | NOS | 870892 | NR | Official entry 8708 92 names silencers (mufflers) and exhaust pipes and parts.
Exhaust Manifold | Header | Exhaust manifold. | Engine type | codes=840991,840999
Exhaust Pipe | Mid Pipe, Down Pipe | Exhaust pipe. | Section | st=C
Muffler | Silencer | Silencer. | Type | st=C
Resonator | Exhaust Resonator | Resonator. | Type
Exhaust Flex Pipe | Flexible Exhaust Joint | Flex joint. | Length
Exhaust Gasket | Manifold Gasket | Exhaust gasket. | Type | codes=848410,401693
Exhaust Hanger | Exhaust Mounting Rubber | Hanger. | Type | codes=401699,870892
Catalytic Converter | Cat, Three-Way Catalyst | Catalytic converter. | Type | codes=842132&st=C&note=Official entry 8421 32 names catalytic converters or particulate filters.
SCR Dosing Pump | AdBlue Pump | Dosing pump for SCR. | Vehicle fit | codes=841370,848180
## Belts, Bearings & Seals | Belts | Drive | PART | NOS | 401031,401032,401033,401034 | NR | Official 4010 entries split by cross-section, ribbing and outside circumference: confirm.
Serpentine Belt | Multi-Rib Belt, Auxiliary Belt | V-ribbed accessory belt. | Ribs; Length
V-Belt | Fan Belt | V belt. | Width; Length | codes=401032,401034
Power Steering Belt | PS Belt | Belt for power steering. | Length
AC Belt | Compressor Belt | Belt for A/C compressor. | Length
## Belts, Bearings & Seals | Gaskets, Seals & Hardware | Sealing | PART | NOS | 401693 | NR | Official entry 4016 93 names gaskets, washers and other seals of vulcanised rubber; metal-rubber seals may differ.
O-Ring Assortment | O-Ring Kit | O-ring assortment. | Size range | unit=KIT&st=C
Oil Seal Assortment | Seal Kit | Oil seal assortment. | Size range | unit=KIT&codes=848420,401693
Gearbox Output Seal | Output Shaft Seal | Gearbox seal. | Size | codes=848420,401693
Axle Seal | Differential Pinion Seal | Axle seal. | Size | codes=848420,401693
RTV Gasket Maker | Silicone Sealant | RTV sealant. | Pack size | unit=ML&codes=321410&st=C&note=Official entry 3214 10 names caulking compounds and other mastics.
Thread Locker | Threadlock | Thread locking adhesive. | Strength | unit=ML&codes=350691,350699&st=NR
Copper Sealing Washer | Copper Washer | Sealing washer. | Size | unit=PKT&codes=731822
Banjo Bolt | Banjo Fitting | Banjo bolt. | Thread | codes=731815
Hose Clamp | Jubilee Clip | Hose clamp. | Size | unit=PKT&codes=&st=U&note=Clamp classification depends on material and design: no entry selected.
Cotter Pin | Split Pin | Cotter pin. | Size | unit=PKT&codes=731824&st=C
Circlip Assortment | Snap Ring Kit | Circlip assortment. | Size range | unit=KIT&codes=731829&st=NR
Rivet | Pop Rivet | Rivet. | Size | unit=PKT&codes=731823&st=C
Self-Tapping Screw | Tapping Screw | Screw. | Size | unit=PKT&codes=731814&st=C
Nut and Bolt Assortment | Fastener Assortment | Fastener assortment. | Size range | unit=KIT&codes=731815,731816&st=NR
Washer Assortment | Flat Washer Kit | Washer assortment. | Size range | unit=KIT&codes=731822&st=NR
Trim Clip Assortment | Body Clips, Push Clips | Plastic clips. | Size range | unit=KIT&codes=&st=U&note=Plastic clips are articles of plastics: no entry selected without material and use.
Cable Tie | Zip Tie | Cable tie. | Length | unit=PKT&codes=&st=U&note=Articles of plastics: no entry selected without material.
## Detailing & Cleaning | Detailing Products | Cleaning | DETAILING_PRODUCT | ML | 340290 | NR | Preparations classification depends on composition and retail pack: confirm.
Car Shampoo | Wash Shampoo | Car wash shampoo. | Pack size | codes=340250,340290
Car Polish | Body Polish, Wax | Polish for coachwork. | Type: polish/wax | codes=340530&st=C&note=Official entry 3405 30 names polishes for coachwork.
Rubbing Compound | Cutting Compound | Abrasive paint compound. | Grade | codes=340530,340540
Glass Cleaner | Windshield Cleaner | Glass cleaner. | Pack size | codes=340290,340590
Tyre Dressing | Tyre Shine | Tyre dressing. | Pack size | codes=340590
Dashboard Polish | Interior Dressing | Interior dressing. | Pack size | codes=340590
Upholstery Cleaner | Fabric Cleaner | Upholstery cleaner. | Pack size
Leather Conditioner | Leather Care | Leather conditioner. | Pack size | codes=340510&st=NR
Engine Degreaser | Engine Cleaner | Degreaser. | Pack size | codes=340290,381400
Brake Cleaner | Brake Parts Cleaner | Solvent for brake parts. | Pack size | codes=381400,271019&st=NR
Throttle Body Cleaner | Carb Cleaner | Throttle cleaner. | Pack size | codes=381400&st=NR
Contact Cleaner | Electrical Cleaner | Electrical contact cleaner. | Pack size | codes=381400&st=NR
Anti-Rust Coating | Rust Proofing | Rust proofing. | Pack size | codes=340319,320810&st=NR
Underbody Coating | Underseal | Underbody coating. | Pack size | codes=320810,340319&st=NR
Microfibre Cloth | Detailing Cloth | Cleaning cloth. | Size | unit=NOS&codes=630710&st=C&note=Official entry 6307 10 names floor-cloths, dish-cloths, dusters and similar cleaning cloths.
## Detailing & Cleaning | Workshop Consumables | Workshop | WORKSHOP_CONSUMABLE | NOS | | U | Used up in the workshop and normally billed as other charges (estimate type OTHERS), not as a catalog part.
Shop Towel | Workshop Rags, Wiping Cloth | Wiping cloth. | Pack | unit=PKT
Masking Tape | Painters Tape | Masking tape. | Width | unit=ROLL
Sandpaper | Abrasive Paper | Abrasive paper. | Grit | unit=PKT
Polishing Pad | Buffing Pad | Buffing pad. | Diameter
Paint Brush | Detail Brush | Brush. | Type | codes=960340&st=NR
Nitrile Gloves | Workshop Gloves | Gloves. | Size | unit=BOX
Body Filler | Dent Putty, Polyester Putty | Body filler. | Pack size | unit=KG&codes=321410&st=NR
## Paint & Body Materials | Paints | Body Shop | PART | LTR | 320810,320890,321000 | NR | Automotive paints fall under different 32xx headings by binder and form: confirm from the product data sheet.
Automotive Primer | Surfacer, Filler Primer | Primer. | Type: 1K/2K; Pack size
Base Coat | Colour Coat, Basecoat | Base coat. | Colour; Pack size
Clear Coat | Lacquer, Clearcoat | Clear coat. | Type: 1K/2K; Pack size
Paint Thinner | Reducer, Solvent | Thinner. | Type | codes=381400&st=NR
Hardener | Activator | Paint hardener. | Pack size
## Electric Vehicle Components | Traction System | EV | EV_SPECIALIST | NOS | | NR | Specialist high-voltage parts: handling and classification need specialist and tax-professional review; no single official entry assumed.
Traction Battery Pack | EV Battery, HV Battery | High-voltage traction battery. | Chemistry; Capacity kWh; Voltage | codes=850760
Battery Module | Cell Module | Battery module. | Chemistry; Voltage | codes=850760
Battery Management System | BMS | Battery management unit. | Vehicle fit | codes=853710
Traction Motor | EV Drive Motor | EV drive motor. | Power kW; Type | codes=850131,850132,850152
Motor Controller | Inverter | Motor controller. | Voltage; Power | codes=850440
DC-DC Converter | HV to LV Converter | DC-DC converter. | Voltage | codes=850440
On-Board Charger | OBC | On-board charger. | Power kW | codes=850440
Charging Inlet | Charge Port | Charging port. | Type: AC/DC | codes=853669
Charging Cable | EVSE Cable | Charging cable. | Type; Length | codes=854442,854449
High Voltage Cable | HV Harness | HV cable. | Length | codes=854442,854449
High Voltage Contactor | HV Relay | HV contactor. | Rating | codes=853649
Battery Coolant Pump | Thermal Pump | Pump for battery cooling. | Voltage | codes=841370
EV Coolant | Battery Coolant | EV coolant. | Pack size | unit=LTR&cls=FLUID_LUBRICANT&codes=382000
EV Reduction Gear Oil | EV Transmission Fluid | Reduction gear oil. | Specification | unit=LTR&cls=FLUID_LUBRICANT&codes=271019,340319
12V Auxiliary Battery | EV 12V Battery | 12V battery. | Type | codes=850710,850720
## Two-Wheeler Parts | Motorcycle Components | Two-Wheeler | PART | NOS | 871410 | NR | Official entry 8714 10 names parts and accessories of motorcycles (including mopeds); confirm the specific component.
Drive Chain | Motorcycle Chain | Motorcycle drive chain. | Pitch; Links; Type: O-ring | codes=731511&st=NR
Sprocket Set | Chain Sprocket Kit | Front and rear sprockets. | Teeth | unit=SET&codes=848390,871410
Motorcycle Clutch Plate Set | Bike Clutch Plates | Clutch plates. | Plates | unit=SET
Motorcycle Brake Shoe | Bike Brake Shoes | Motorcycle brake shoes. | Position | unit=SET&codes=871494&st=C
Motorcycle Brake Pad Set | Bike Brake Pads | Motorcycle disc pads. | Position | unit=SET&codes=871494,681381&st=NR
Throttle Cable | Accelerator Cable | Throttle cable. | Length | codes=871410,731210
Front Fork Assembly | Telescopic Fork | Front fork. | Vehicle fit | codes=871491&st=C
Fork Oil Seal | Fork Seal | Fork seal. | Size | unit=PAIR&codes=848420,401693
Rear Shock Absorber - Motorcycle | Bike Rear Shocks | Rear shock absorber. | Type | unit=PAIR&codes=871410
Motorcycle Wheel Rim | Bike Rim | Wheel rim. | Size | codes=871492&st=C
Motorcycle Spoke Set | Spokes | Spokes. | Size | unit=SET&codes=871492&st=C
Motorcycle Seat | Saddle | Seat. | Vehicle fit | codes=871410&st=NR
Motorcycle Handlebar | Handle Bar | Handlebar. | Vehicle fit | codes=871410
Motorcycle Rectifier | Regulator Rectifier | Regulator rectifier. | Voltage | codes=850440,871410
CDI Unit | Ignition CDI | CDI unit. | Vehicle fit | codes=853710,871410
## Commercial Vehicle Parts | Heavy Vehicle Components | Commercial | PART | NOS | 870830,870899 | NR | Heavy-vehicle parts are classified like other motor-vehicle parts: confirm the specific entry.
Air Brake Chamber | Brake Chamber | Air brake chamber. | Type | codes=870830
Air Dryer | Air Brake Dryer | Air dryer. | Vehicle fit | codes=841410,841480,870830
Air Brake Compressor | Truck Compressor | Air compressor. | Vehicle fit | codes=841480
Brake Lining | Riveted Lining | Brake lining. | Width | unit=SET&codes=681381,870830&st=NR
Spring Bush | Leaf Spring Bush | Spring bush. | Size | codes=870880
Tow Hook | Towing Eye | Tow hook. | Vehicle fit | codes=870899
Tow Bar | Tow Hitch | Tow bar. | Vehicle fit | codes=870899
Axle Shaft | Rear Axle Shaft | Axle shaft. | Length | codes=870850
## Accessories & Audio | Audio, Camera & Safety Accessories | Accessories | PART | NOS | 852721,851821 | NR | Research-derived addition (coverage audit). Vehicle audio and camera equipment classification depends on the exact product: confirm.
Car Audio Head Unit | Car Stereo, Music System, Infotainment Head Unit | Vehicle audio or infotainment head unit. | Size: single/double DIN; Features | codes=852721,852729
Car Speaker | Door Speaker, Loudspeaker | Vehicle loudspeaker. | Size; Power; Type: coaxial/component | codes=851821,851829
Car Antenna | Aerial, Shark Fin Antenna | Radio or signal antenna. | Type: mast/shark fin | codes=852910
Dash Camera | Dashcam, Vehicle Camera Recorder | Vehicle video recorder. | Channels: single/dual | codes=852589,852580
Fire Extinguisher - Vehicle | Car Fire Extinguisher | Portable fire extinguisher for vehicles. | Capacity; Type | codes=842410&st=C&note=Research-derived addition (coverage audit). Official entry 8424 10 names fire extinguishers: confirm.
Hydraulic Jack | Bottle Jack, Trolley Jack | Hydraulic jack for lifting vehicles. | Capacity | codes=842542&cls=PART&note=Research-derived addition (coverage audit). Workshop tool or consumer accessory: confirm.
## Accessories & Audio | Body & Utility Accessories | Accessories | PART | NOS | 870829,870899 | NR | Research-derived addition (coverage audit). Confirm the entry for the exact accessory.
Roof Rack | Roof Carrier, Luggage Rack | Roof-mounted carrier. | Material; Load rating | codes=870829,870899
Steering Wheel Cover | Steering Cover | Cover for steering wheel rim. | Size; Material | codes=870894,392690
Gear Knob | Gear Shift Knob | Gear lever knob. | Thread; Material | codes=870840,870899
Pedal Assembly | Accelerator Pedal, Brake Pedal | Control pedal. | Type: accelerator/brake/clutch | codes=870899,870830
Exhaust Heat Shield | Heat Shield | Heat shield for exhaust components. | Material | codes=870892,870899
Reflector | Rear Reflector | Reflector fitted to the vehicle. | Type; Colour | codes=870899
Vehicle Cover | Car Body Cover | Protective cover for the vehicle. | Size; Material | codes=630790&note=Research-derived addition (coverage audit). Made-up article: confirm the material-based entry.
Sunshade | Windscreen Sun Shade | Windscreen sunshade. | Size | codes=630790,870899
"""
