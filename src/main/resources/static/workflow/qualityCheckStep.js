window.QualityCheckStep = {

    render() {

        return `

<div class="card shadow-sm">

    <div class="card-header">

        <h4>Quality Check</h4>

        <small class="text-muted">

            Verify repair work before invoice generation.

        </small>

    </div>

    <div class="card-body">

        <div class="form-check mb-2">
            <input class="form-check-input" type="checkbox">
            <label class="form-check-label">
                Road Test Completed
            </label>
        </div>

        <div class="form-check mb-2">
            <input class="form-check-input" type="checkbox">
            <label class="form-check-label">
                No Fluid Leakage
            </label>
        </div>

        <div class="form-check mb-4">
            <input class="form-check-input" type="checkbox">
            <label class="form-check-label">
                Vehicle Cleaned
            </label>
        </div>

        <div class="d-flex justify-content-between">

            <button
                id="previousBtn"
                class="btn btn-outline-secondary">

                Previous

            </button>

            <button
                id="qualityCheckBtn"
                class="btn btn-success">

                Complete Quality Check

            </button>

        </div>

    </div>

</div>

`;

    },

    bindEvents() {

        document
            .getElementById("qualityCheckBtn")
            ?.addEventListener(
                "click",
                async ()=>{

                    await WorkflowService.performQualityCheck();

                    alert("Quality Check Completed.");

                    Workflow.nextStep();

                });

    }

};