window.PortalConfig={

    portal:"EMPLOYEE",

    homePage:"dashboard",

    title:"GarageOS Employee Portal",

    menu:[

        {

            page:"dashboard",

            title:"Dashboard",

            icon:"bi-speedometer2",

            roles:[
                "MANAGER",
                "SERVICE_ADVISOR",
                "TECHNICIAN",
                "ACCOUNTANT",
                "CASHIER",
                "INVENTORY_MANAGER"
            ]

        },

        {

            page:"workflow",

            title:"Start Service",

            icon:"bi-car-front-fill",

            roles:[
                "MANAGER",
                "SERVICE_ADVISOR"
            ]

        },

        {

            page:"activeJobs",

            title:"Active Jobs",

            icon:"bi-list-task",

            roles:[
                "MANAGER",
                "SERVICE_ADVISOR",
                "TECHNICIAN"
            ]

        },

        {

            page:"customers",

            title:"Customers",

            icon:"bi-people",

            roles:[
                "MANAGER",
                "SERVICE_ADVISOR"
            ]

        },

        {

            page:"vehicles",

            title:"Vehicles",

            icon:"bi-car-front",

            roles:[
                "MANAGER",
                "SERVICE_ADVISOR"
            ]

        }

    ]

};