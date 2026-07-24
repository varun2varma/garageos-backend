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

            const response =
                await VehicleService.createVehicle(request);

            const vehicle = response.data;

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

            const response =
                await JobCardService.createJob(request);

            const job = response.data;

            WorkflowHelper.state.job = job;
            WorkflowHelper.state.jobCardId = job.id;
            WorkflowHelper.state.jobCardNumber = job.jobCardNumber;

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

            const responseDump =
                await InspectionService.createInspection(
                    complaintId,
                    request
                );
              const response = responseDump.data
            if (!WorkflowHelper.state.inspections) {

                WorkflowHelper.state.inspections = [];

            }

            const index =
                WorkflowHelper.state.inspections.findIndex(

                    inspection =>
                        inspection.id === response.id

                );

            if (index >= 0) {

                WorkflowHelper.state.inspections[index] =
                    response;

            }

            else {

                WorkflowHelper.state.inspections.push(response);

            }

            return response;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async createEstimate(request) {

        try {

            const response =
                await EstimateService.createEstimate(request);

            const estimate = response.data;

            WorkflowHelper.state.estimate = estimate;
            WorkflowHelper.state.estimateId = estimate.id;

            console.log(
                "Estimate Saved",
                estimate
            );

            return estimate;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async approveEstimate(jobCardNumber) {

        try {

            const response =
                await EstimateService.approveEstimate(
                    jobCardNumber
                );

            WorkflowHelper.state.estimate = response.data;

            return response;

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

            return response;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async addEstimateItem(request) {

        try {

            const response =
                await EstimateItemService.addItem(

                    WorkflowHelper.state.estimateId,

                    request

                );

            const item = response.data;

            if (!WorkflowHelper.state.estimateItems) {

                WorkflowHelper.state.estimateItems = [];

            }

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

            WorkflowHelper.state.estimateItems = response.data;

            return response.data;

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

        return Promise.resolve();

    },

    async finishEstimate(estimateId) {

        const request = {
            jobCardId: WorkflowHelper.state.jobCardId,
            remarks: WorkflowHelper.state.estimateRemarks
        };

        const response = await fetch(
            `/api/v1/estimates/${estimateId}`,
            {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json"
                },
                body: JSON.stringify(request)
            }
        );

        if (!response.ok) {
            throw new Error("Failed to finish estimate.");
        }

        return response.json();
    },


    async startRepair() {

        try {

            const response =
                await Api.post(
                    `/workflow/${WorkflowHelper.state.jobCardNumber}/repair/start`
                );

            WorkflowHelper.state.job = response.data;

            return response.data;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async completeRepair() {

        try {

            const response =
                await Api.post(
                    `/workflow/${WorkflowHelper.state.jobCardNumber}/repair/complete`
                );

            WorkflowHelper.state.job = response.data;

            return response.data;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async performQualityCheck() {

        try {

            const response =
                await Api.post(
                    `/workflow/${WorkflowHelper.state.jobCardNumber}/quality-check`
                );

            WorkflowHelper.state.job = response.data;

            return response.data;

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

            WorkflowHelper.state.invoice = response.data;

            return response.data;

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

            WorkflowHelper.state.job = response.data;

            return response.data;

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

            WorkflowHelper.state.job = response.data;

            return response.data;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async closeJob() {

        try {

            const response =
                await Api.post(
                    `/workflow/${WorkflowHelper.state.jobCardNumber}/close`
                );

            WorkflowHelper.state.job = response.data;

            return response.data;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },


    async loadRepairTasks() {

        try {

            const response =
                await Api.get(
                    `/workflow/${WorkflowHelper.state.jobCardNumber}/repair-tasks`
                );

            WorkflowHelper.state.repairTasks = response.data;

            return response.data;

        } catch (e) {

            console.error(e);

            throw e;

        }

    },

    async getWorkflowStatus() {

        const response =
            await Api.get(
                `/workflow/${WorkflowHelper.state.jobCardNumber}/status`
            );

        return response.data;

    },

};