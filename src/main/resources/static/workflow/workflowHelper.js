window.WorkflowHelper = {

    state: {

        customerId: null,
        customer: null,

        vehicleId: null,
        vehicle: null,

        jobCardId: null,
        jobCard: null,
        jobCardNumber: null,

        complaintIds: [],

        inspections: [],

        estimateId: null,
        estimate: null,

        estimateItems: [],

        invoiceId: null,
        invoice: null

    },

    reset() {

        this.state = {

            customerId: null,
            customer: null,

            vehicleId: null,
            vehicle: null,

            jobCardId: null,
            jobCard: null,
            jobCardNumber: null,

            complaintIds: [],

            inspections: [],

            estimateId: null,
            estimate: null,

            estimateItems: [],

            invoiceId: null,
            invoice: null

        };

    },

    isCustomerCreated() {

        return this.state.customerId !== null;

    },

    isVehicleCreated() {

        return this.state.vehicleId !== null;

    },

    isJobCreated() {

        return this.state.jobCardId !== null;

    },

    isEstimateCreated() {

        return this.state.estimateId !== null;

    },

    isInvoiceCreated() {

        return this.state.invoiceId !== null;

    },

    addComplaintId(id) {

        if (!this.state.complaintIds.includes(id)) {

            this.state.complaintIds.push(id);

        }

    },

    removeComplaintId(id) {

        this.state.complaintIds =
            this.state.complaintIds.filter(x => x !== id);

    },

    getCurrentComplaintId() {

        if (this.state.complaintIds.length === 0) {

            return null;

        }

        return this.state.complaintIds[0];

    },

    addInspection(inspection) {

        this.state.inspections.push(inspection);

    },

    addEstimateItem(item) {

        this.state.estimateItems.push(item);

    },

    removeEstimateItem(itemId) {

        this.state.estimateItems =
            this.state.estimateItems.filter(

                item => item.id !== itemId

            );

    },

    clearEstimateItems() {

        this.state.estimateItems = [];

    },

    getEstimateGrandTotal() {

        return this.state.estimateItems.reduce(

            (sum, item) => {

                return sum + Number(item.totalPrice);

            },

            0

        );

    },

    printState() {

        console.table(this.state);

    }

};