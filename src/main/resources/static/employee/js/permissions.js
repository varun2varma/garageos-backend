/*
==========================================================
 GarageOS Employee Permissions
==========================================================
*/

window.Permissions = {

    has(role) {

        return EmployeeSession

            .roles()

            .includes(role);

    },

    hasAny(...roles) {

        return roles.some(

            role => this.has(role)

        );

    },

    hasAll(...roles) {

        return roles.every(

            role => this.has(role)

        );

    },

    isManager() {

        return this.has(Roles.MANAGER);

    },

    isServiceAdvisor() {

        return this.has(Roles.SERVICE_ADVISOR);

    },

    isTechnician() {

        return this.has(Roles.TECHNICIAN);

    },

    isCashier() {

        return this.has(Roles.CASHIER);

    },

    isAccountant() {

        return this.has(Roles.ACCOUNTANT);

    },

    isInventoryManager() {

        return this.has(

            Roles.INVENTORY_MANAGER

        );

    },

    canCreateJobCard() {

        return this.hasAny(

            Roles.MANAGER,

            Roles.SERVICE_ADVISOR

        );

    },

    canInspection() {

        return this.hasAny(

            Roles.MANAGER,

            Roles.SERVICE_ADVISOR,

            Roles.TECHNICIAN

        );

    },

    canEstimate() {

        return this.hasAny(

            Roles.MANAGER,

            Roles.SERVICE_ADVISOR

        );

    },

    canApproveEstimate() {

        return this.has(

            Roles.MANAGER

        );

    },

    canRepair() {

        return this.hasAny(

            Roles.MANAGER,

            Roles.TECHNICIAN

        );

    },

    canQualityCheck() {

        return this.hasAny(

            Roles.MANAGER,

            Roles.TECHNICIAN

        );

    },

    canInvoice() {

        return this.hasAny(

            Roles.MANAGER,

            Roles.ACCOUNTANT

        );

    },

    canPayment() {

        return this.hasAny(

            Roles.MANAGER,

            Roles.ACCOUNTANT,

            Roles.CASHIER

        );

    },

    canDelivery() {

        return this.hasAny(

            Roles.MANAGER,

            Roles.SERVICE_ADVISOR

        );

    }

};