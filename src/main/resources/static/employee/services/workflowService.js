window.WorkflowService = {

    async createCustomer(request) {

        try {

            const response =
                await CustomerService.createCustomer(request);

            WorkflowHelper.state.customer = response;
            WorkflowHelper.state.customerId = response.id;

            return response;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async createVehicle(request) {

        try {

            const vehicle =
                await VehicleService.createVehicle(request);

            WorkflowHelper.state.vehicle = vehicle;
            WorkflowHelper.state.vehicleId = vehicle.id;

            return vehicle;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async createJob(request) {

        try {

            const job =
                await JobCardService.createJob(request);

            WorkflowHelper.state.job = job;
            WorkflowHelper.state.jobCardId = job.id;
            WorkflowHelper.state.jobCardNumber = job.jobCardNumber;
            await this.refreshWorkflowStatus();

            return job;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async createInspection(
        complaintId,
        request
    ) {

        try {

            const inspection = await InspectionService.createInspection(
                    complaintId,
                    request
                );

            if (!WorkflowHelper.state.inspections) {

                WorkflowHelper.state.inspections = [];

            }

            const index =
                WorkflowHelper.state.inspections.findIndex(
                    existing => existing.id === inspection.id
                );

            if (index >= 0) {

                WorkflowHelper.state.inspections[index] =
                    inspection;

            }

            else {

                WorkflowHelper.state.inspections.push(inspection);

            }
            await this.refreshWorkflowStatus();

            return inspection;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async createEstimate(request) {

        try {

            const estimate =
                await EstimateService.createEstimate(request);

            WorkflowHelper.state.estimate = estimate;
            WorkflowHelper.state.estimateId = estimate.id;

            console.log(
                "Estimate Saved",
                estimate
            );
            await this.refreshWorkflowStatus();

            return estimate;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async approveEstimate(jobCardNumber) {

        try {

            const estimate =
                await EstimateService.approveEstimate(jobCardNumber);

            WorkflowHelper.state.estimate = estimate;
            await this.refreshWorkflowStatus();

            await this.loadRepairTasks();

            return estimate;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async rejectEstimate() {

        try {

            const response =
                await EstimateService.rejectEstimate(

                    WorkflowHelper.state.estimateId

                );

            WorkflowHelper.state.estimate = response;
            await this.refreshWorkflowStatus();

            return response;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async addEstimateItem(request) {

        try {

            const item =
                await EstimateItemService.addItem(

                    WorkflowHelper.state.estimateId,

                    request

                );

            if (!WorkflowHelper.state.estimateItems) {

                WorkflowHelper.state.estimateItems = [];

            }
            await this.refreshWorkflowStatus();

            await this.loadEstimateItems();

            return item;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async loadEstimateItems() {

        try {

            const response =
                await EstimateItemService.getItems(

                    WorkflowHelper.state.estimateId

                );

            await this.refreshWorkflowStatus();

            WorkflowHelper.state.estimateItems = response;

            return response;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async updateEstimateItem(
        itemId,
        request
    ) {

        try {

            return await EstimateItemService.updateItem(

                itemId,

                request

            );

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async deleteEstimateItem(itemId) {

        try {

            await EstimateItemService.deleteItem(itemId);
            await this.refreshWorkflowStatus();

            await this.loadEstimateItems();
//            WorkflowHelper.state.estimateItems =
//                WorkflowHelper.state.estimateItems.filter(
//
//                    item => item.id !== itemId
//
//                );

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async createInvoice(request) {

        try {

            const response =
                await InvoiceService.createInvoice(request);
                await this.refreshWorkflowStatus();

            WorkflowHelper.state.invoice = response;
            WorkflowHelper.state.invoiceId = response.id;

            return response;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async generateInvoice() {

        console.log("Generating Invoice...");
        await this.refreshWorkflowStatus();

        return Promise.resolve();

    },

    async refreshInvoice() {

        if (!WorkflowHelper.state.invoiceId) {

            return null;

        }

        const invoice =
            await InvoiceService.getInvoice(
                WorkflowHelper.state.invoiceId
            );

        WorkflowHelper.state.invoice =
            invoice;

        return invoice;

    },

    async finishEstimate(estimateId) {

        const request = {
            jobCardId: WorkflowHelper.state.jobCardId,
            remarks: WorkflowHelper.state.estimateRemarks
        };

        const response = await Api.put(
            `/estimates/${estimateId}`,
            request
        );

        await this.refreshWorkflowStatus();

        return response;
    },


async startRepair(assignmentId, request = {}) {

    try {

        const response =
            await JobAssignmentService.start(
                assignmentId,
                request
            );

        /*
        -----------------------------------------
        Update assignment state
        -----------------------------------------
        */

        const assignments =
            WorkflowHelper.state.assignments || [];

        const index =
            assignments.findIndex(
                assignment =>
                    assignment.id === response.id
            );

        if (index >= 0) {

            assignments[index] = response;

        }

        /*
        -----------------------------------------
        Refresh workflow status
        -----------------------------------------
        */

        await this.refreshWorkflowStatus();

        return response;

    } catch (e) {

        console.error(
            "Unable to start assignment",
            e
        );

        throw e;

    }

},


async completeRepair(
    assignmentId,
    request = {}
) {

    try {

        const response =
            await JobAssignmentService.complete(
                assignmentId,
                request
            );

        /*
        -----------------------------------------
        Update assignment state
        -----------------------------------------
        */

        const assignments =
            WorkflowHelper.state.assignments || [];

        const index =
            assignments.findIndex(
                assignment =>
                    assignment.id === response.id
            );

        if (index >= 0) {

            assignments[index] = response;

        }

        /*
        -----------------------------------------
        Refresh workflow status
        -----------------------------------------
        */

        await this.refreshWorkflowStatus();

        return response;

    } catch (e) {

        console.error(
            "Unable to complete assignment",
            e
        );

        throw e;

    }

},

//    async startRepair(repairTaskId) {
//
//        try {
//
//            const response =
//                await Api.put(
//                    `/repair-tasks/${repairTaskId}/start`
//                );
//console.log("API Response", response);
//            const index =
//                WorkflowHelper.state.repairTasks.findIndex(
//                    task => task.id === response.id
//                );
//console.log("Index", index);
//            if (index >= 0) {
//
//                WorkflowHelper.state.repairTasks[index] =
//                    response;
//
//            }
//            console.log(
//                WorkflowHelper.state.repairTasks);
//
//                await this.refreshWorkflowStatus();
//
//            return response;
//
//        } catch (e) {
//
//            console.error(e);
//
//            throw e;
//
//        }
//
//    },
//
//    async completeRepair(repairTaskId) {
//
//        try {
//
//            const response =
//                await Api.put(
//                    `/repair-tasks/${repairTaskId}/complete`
//                );
//
//                console.log("API Response", response);
//
//            const index =
//                WorkflowHelper.state.repairTasks.findIndex(
//                    task => task.id === response.id
//                );
//console.log("Index", index);
//await this.refreshWorkflowStatus();
//
//            if (index >= 0) {
//
//                WorkflowHelper.state.repairTasks[index] =
//                    response;
//
//            }
//
//            console.log(
//                WorkflowHelper.state.repairTasks);
//            return response;
//
//        } catch (e) {
//
//            console.error(e);
//
//            throw e;
//
//        }
//
//    },

    async performQualityCheck() {

        try {

            const response =
                await Api.post(
                    `/workflow/${WorkflowHelper.state.jobCardNumber}/quality-check`
                );

            WorkflowHelper.state.job = response;
            await this.refreshWorkflowStatus();

            return response;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async generateWorkflowInvoice() {

        try {

            const response =
                await Api.post(
                    `/workflow/${WorkflowHelper.state.jobCardNumber}/invoice`
                );

            WorkflowHelper.state.invoice = response;
            await this.refreshWorkflowStatus();

            return response;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async receivePayment() {

        try {

            const response =
                await Api.post(
                    `/workflow/${WorkflowHelper.state.jobCardNumber}/payment`
                );

            WorkflowHelper.state.job = response;
            await this.refreshWorkflowStatus();

            return response;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async readyForDelivery() {

        try {

            const response =
                await Api.post(
                    `/workflow/${WorkflowHelper.state.jobCardNumber}/delivery`
                );

            WorkflowHelper.state.job = response;
            await this.refreshWorkflowStatus();

            return response;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async refreshWorkflowStatus() {

        WorkflowHelper.state.workflowStatus =
            await this.getWorkflowStatus();

    },

    syncWorkflowState(data) {

        if (!data) {

            return;

        }

        if (data.workflowStatus !== undefined) {

            WorkflowHelper.state.workflowStatus =
                data.workflowStatus;

        }

        if (data.customer !== undefined) {

            WorkflowHelper.state.customer = data.customer;
            WorkflowHelper.state.customerId = data.customer?.id ?? null;

        }

        if (data.vehicle !== undefined) {

            WorkflowHelper.state.vehicle = data.vehicle;
            WorkflowHelper.state.vehicleId = data.vehicle?.id ?? null;

        }

        if (data.job !== undefined) {

            WorkflowHelper.state.job = data.job;
            WorkflowHelper.state.jobCardId = data.job?.id ?? null;
            WorkflowHelper.state.jobCardNumber =
                data.job?.jobCardNumber ?? null;

        }

        if (data.complaints !== undefined) {

            WorkflowHelper.state.complaints = data.complaints;

        }

        if (data.inspections !== undefined) {

            WorkflowHelper.state.inspections = data.inspections;

        }

        if (data.estimate !== undefined) {

            WorkflowHelper.state.estimate = data.estimate;
            WorkflowHelper.state.estimateId = data.estimate?.id ?? null;

        }

        if (data.estimateItems !== undefined) {

            WorkflowHelper.state.estimateItems = data.estimateItems;

        }

        if (data.repairTasks !== undefined) {

            WorkflowHelper.state.repairTasks = data.repairTasks;

        }

        if (data.invoice !== undefined) {

            WorkflowHelper.state.invoice = data.invoice;
            WorkflowHelper.state.invoiceId = data.invoice?.id ?? null;

        }

    },

    async closeJob() {

        try {

            const response =
                await Api.post(
                    `/workflow/${WorkflowHelper.state.jobCardNumber}/close`
                );

            WorkflowHelper.state.job = response;

            return response;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },


//    async loadRepairTasks() {
//
//        try {
//
//            const response =
//                await Api.get(
//                    `/workflow/${WorkflowHelper.state.jobCardNumber}/repair-tasks`
//                );
//
//            WorkflowHelper.state.repairTasks = response;
//
//            return response;
//
//        } catch (e) {
//
//            console.error(e);
//
//            throw e;
//
//        }
//
//    },

    async getWorkflowStatus() {

        const response =
            await Api.get(
                `/workflow/${WorkflowHelper.state.jobCardNumber}/status`
            );

        return response;

    },

    async resumeWorkflow(jobCardNumber) {

        try {

            const data =
                await Api.get(`/workflow/${jobCardNumber}/resume`);

            this.syncWorkflowState(data);

            return data;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async assignTechnician(
        repairTaskId,
        technicianName
    ) {

        try {

            const response =
                await Api.put(
                    `/repair-tasks/${repairTaskId}/assign`,
                    {
                        technicianName
                    }
                );

            const index =
                WorkflowHelper.state.repairTasks.findIndex(
                    task => task.id === response.id
                );

            if (index >= 0) {

                WorkflowHelper.state.repairTasks[index] =
                    response;

            }

            return response;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async loadRepairTasks() {

        try {

            const response =
                await Api.get(
                    `/repair-tasks/jobcards/${WorkflowHelper.state.jobCardId}`
                );

            WorkflowHelper.state.repairTasks = response;

            return response;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async loadAssignments() {

        const assignments =
            await JobAssignmentService.getByJobCard(
                WorkflowHelper.state.job.id
            );

        WorkflowHelper.state.assignments =
            assignments;

        return assignments;

    },



};