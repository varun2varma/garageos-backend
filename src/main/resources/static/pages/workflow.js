window.Workflow = {

    currentStep: 1,

    totalSteps: 13,

    render() {

        return `

    <div class="fade-in">

        <div class="container-fluid">

            <div id="workflowHeader"></div>

            <div id="workflowStepper" class="mt-3"></div>

            <div id="workflowContent" class="mt-4"></div>

        </div>

    </div>

    `;

    },

    async bindEvents() {

        await this.renderStep();

    },

//    renderStep() {
//
//        document.getElementById("workflowStepper").innerHTML =
//            Stepper.render(
//                this.currentStep,
//                this.totalSteps
//            );
//
//        document.getElementById("workflowContent").innerHTML =
//            this.getStepContent();
//
//        this.bindStepEvents();
//
//    },

    async renderStep() {

        await this.renderHeader();

        document.getElementById("workflowStepper").innerHTML =
            Stepper.render(
                this.currentStep,
                this.totalSteps
            );

        document.getElementById("workflowContent").innerHTML =
            this.getStepContent();

        this.bindStepEvents();

    },


    async renderHeader() {

        if (!WorkflowHelper.state.jobCardNumber) {

            document.getElementById("workflowHeader").innerHTML = "";

            return;

        }

        const workflow =
            await WorkflowService.getWorkflowStatus();

        document.getElementById("workflowHeader").innerHTML = `

    <div class="card shadow-sm">

        <div class="card-body">

            <div class="d-flex justify-content-between">

                <div>

                    <h5 class="mb-1">

                        Job Card :
                        ${workflow.jobCardNumber}

                    </h5>

                    <small class="text-muted">

                        Current Status :
                        ${workflow.status}

                    </small>

                </div>

                <h4>

                    ${workflow.progress}%

                </h4>

            </div>

            <div class="progress mt-3">

                <div
                    class="progress-bar progress-bar-striped progress-bar-animated"
                    style="width:${workflow.progress}%">

                </div>

            </div>

        </div>

    </div>

    `;

    },

    getStepContent() {

        switch (this.currentStep) {

            case 1:
                return CustomerStep.render();

            case 2:
                return VehicleStep.render();

            case 3:
                return JobStep.render();

            case 4:
                return InspectionStep.render();

            case 5:
                return EstimateStep.render();

            case 6:
                return EstimateItemStep.render();

            case 7:
                return EstimateSummaryStep.render();

            case 8:
                return ApprovalStep.render();

            case 9:
                return RepairStep.render();

            case 10:
                return QualityCheckStep.render();

            case 11:
                return InvoiceStep.render();

            case 12:
                return PaymentStep.render();

            case 13:
                return DeliveryStep.render();

            default:
                return "";

        }

    },

    paymentStep() {

        return `

<div class="card shadow-sm">

    <div class="card-body text-center p-5">

        <h3>

            Payment

        </h3>

        <p class="text-muted">

            Payment module will be implemented in Sprint 5.

        </p>

        <div class="d-flex justify-content-between">

            <button
                id="previousBtn"
                class="btn btn-secondary">

                Previous

            </button>

            <button
                class="btn btn-success"
                disabled>

                Workflow Completed

            </button>

        </div>

    </div>

</div>

`;

    },

    bindStepEvents() {

        document
            .getElementById("previousBtn")
            ?.addEventListener(
                "click",
                () => this.previousStep()
            );

        switch (this.currentStep) {

            case 1:

                CustomerStep.bindEvents();

                break;

            case 2:

                VehicleStep.bindEvents();

                break;

            case 3:

                JobStep.bindEvents();

                break;

            case 4:

                InspectionStep.bindEvents();

                break;

            case 5:

                EstimateStep.bindEvents();

                break;

            case 6:

                WorkflowService
                    .loadEstimateItems()
                    .then(() => {

                        EstimateItemStep.refresh();

                    });

                break;

            case 7:

                EstimateSummaryStep.bindEvents();

                break;

            case 8:
                ApprovalStep.bindEvents();
                break;

            case 9:
                RepairStep.bindEvents();
                break;

            case 10:
                QualityCheckStep.bindEvents();
                break;

            case 11:
                InvoiceStep.bindEvents();
                break;

            case 12:
                PaymentStep.bindEvents();
                break;

            case 13:
                DeliveryStep.bindEvents();
                break;

            case 14:

                break;

        }

    },

    async nextStep() {

        if (this.currentStep < this.totalSteps) {

            this.currentStep++;

            await this.renderStep();

        }

    },

    async previousStep() {

        if (this.currentStep > 1) {

            this.currentStep--;

            await this.renderStep();

        }

    },

    async goToStep(step) {

        if (step < 1 || step > this.totalSteps) {

            return;

        }

        this.currentStep = step;

        await this.renderStep();

    },

    async reset() {

        this.currentStep = 1;

        WorkflowHelper.reset();

        await this.renderStep();

    }

};