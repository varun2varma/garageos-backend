window.Workflow = {

    currentStep: 1,

    get totalSteps() {

        return Stepper.getTotalSteps();

    },

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

        if (WorkflowHelper.state.workflowStatus) {

            this.currentStep =
                WorkflowHelper.state.workflowStatus.nextStep;

        } else {

            this.currentStep = 1;

        }

        await this.renderStep();

    },

    async renderStep() {

        await this.renderHeader();

        document.getElementById("workflowStepper").innerHTML =
            Stepper.render(
                this.currentStep
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

//        const workflow =
//            await WorkflowService.getWorkflowStatus();

        const workflow =
            WorkflowHelper.state.workflowStatus;

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

        const step =

            Stepper.getCurrentStep(

                this.currentStep

            );

        if (!step) {

            return "";

        }

        const component =

            window[step.component];

        if (!component) {

            return `

    <div class="alert alert-danger">

        Component

        <strong>

            ${step.component}

        </strong>

        not found.

    </div>

    `;

        }

        return component.render();

    },

    bindStepEvents() {

        document

            .getElementById("previousBtn")

            ?.addEventListener(

                "click",

                () =>

                    this.previousStep()

            );

        const step =

            Stepper.getCurrentStep(

                this.currentStep

            );

        if (!step) {

            return;

        }

        const component =

            window[step.component];

        if (

            component &&

            component.bindEvents

        ) {

            component.bindEvents();

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