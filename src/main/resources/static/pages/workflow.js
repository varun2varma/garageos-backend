window.Workflow = {

    currentStep: 1,

    totalSteps: 9,

    render() {

        return `

<div class="fade-in">

    <div class="container-fluid">

        <div id="workflowStepper"></div>

        <div id="workflowContent" class="mt-4"></div>

    </div>

</div>

`;

    },

    bindEvents() {

        this.renderStep();

    },

    renderStep() {

        document.getElementById("workflowStepper").innerHTML =
            Stepper.render(
                this.currentStep,
                this.totalSteps
            );

        document.getElementById("workflowContent").innerHTML =
            this.getStepContent();

        this.bindStepEvents();

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
                return InvoiceStep.render();

            case 9:
                return this.paymentStep();

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

                InvoiceStep.bindEvents();

                break;

            case 9:

                break;

        }

    },

    nextStep() {

        if (this.currentStep < this.totalSteps) {

            this.currentStep++;

            this.renderStep();

        }

    },

    previousStep() {

        if (this.currentStep > 1) {

            this.currentStep--;

            this.renderStep();

        }

    },

    goToStep(step) {

        if (step < 1 || step > this.totalSteps) {

            return;

        }

        this.currentStep = step;

        this.renderStep();

    },

    reset() {

        this.currentStep = 1;

        WorkflowHelper.reset();

        this.renderStep();

    }

};