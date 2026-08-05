window.Dashboard = {

    render() {

            if (Permissions.isTechnician()) {

                return TechnicianDashboard.render();

            }

            if (Permissions.isServiceAdvisor()) {

                return ServiceAdvisorDashboard.render();

            }

            return ManagerDashboard.render();

        },

         bindEvents() {

                if (Permissions.isTechnician()) {

                    TechnicianDashboard.bindEvents();
                    return;

                }

                if (Permissions.isServiceAdvisor()) {

                    ServiceAdvisorDashboard.bindEvents();
                    return;

                }

                ManagerDashboard.bindEvents();

         },

         getStatusBadge(status) {

             switch (status) {

                 case "OPEN":
                     return "badge bg-dark";

                 case "INSPECTION_PENDING":
                     return "badge bg-warning text-dark";

                 case "INSPECTION_COMPLETED":
                     return "badge bg-info";

                 case "ESTIMATE_PENDING":
                     return "badge bg-info";

                 case "WAITING_FOR_APPROVAL":
                     return "badge bg-primary";

                 case "ESTIMATE_APPROVED":
                     return "badge bg-success";

                 case "REPAIR_PENDING":
                     return "badge bg-secondary";

                 case "REPAIR_IN_PROGRESS":
                     return "badge bg-primary";

                 case "REPAIR_COMPLETED":
                     return "badge bg-success";

                 case "QUALITY_CHECK":
                     return "badge bg-warning text-dark";

                 case "READY_FOR_INVOICE":
                     return "badge bg-info";

                 case "INVOICE_GENERATED":
                     return "badge bg-success";

                 case "PAYMENT_PENDING":
                     return "badge bg-danger";

                 case "PAYMENT_COMPLETED":
                     return "badge bg-success";

                 case "READY_FOR_DELIVERY":
                     return "badge bg-success";

                 case "DELIVERED":
                     return "badge bg-success";

                 case "WORK_COMPLETED":
                     return "badge bg-success";

                 case "CLOSED":
                     return "badge bg-dark";

                 case "CANCELLED":
                     return "badge bg-danger";

                 default:
                     return "badge bg-secondary";

             }

         },

         formatStatus(status) {

             return status

                 .replaceAll("_", " ")

                 .toLowerCase()

                 .replace(/\b\w/g, c => c.toUpperCase());

         },

    };