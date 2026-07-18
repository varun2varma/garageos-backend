window.Stepper = {

    labels: [
        "Customer",
        "Vehicle",
        "Job Card",
        "Inspection",
        "Estimate",
        "Estimate Items",
        "Estimate Summary",
        "Invoice",
        "Payment"
    ],

    render(currentStep, totalSteps) {

        let html = `
        <div class="card shadow-sm border-0 mb-4">
            <div class="card-body">
                <div class="d-flex justify-content-between align-items-center flex-wrap gap-3">
        `;

        for (let i = 1; i <= totalSteps; i++) {

            const completed = i < currentStep;
            const active = i === currentStep;

            let badgeClass = "bg-secondary";

            if (completed) {
                badgeClass = "bg-success";
            }

            if (active) {
                badgeClass = "bg-primary";
            }

            html += `
                <div class="d-flex align-items-center flex-fill">

                    <div class="text-center">

                        <div
                            class="rounded-circle ${badgeClass} text-white d-flex align-items-center justify-content-center"
                            style="
                                width:42px;
                                height:42px;
                                font-weight:600;
                                margin:auto;
                            ">

                            ${completed ? "✓" : i}

                        </div>

                        <div
                            class="mt-2"
                            style="
                                font-size:12px;
                                font-weight:500;
                                white-space:nowrap;
                            ">

                            ${this.labels[i - 1] || ("Step " + i)}

                        </div>

                    </div>

                    ${i < totalSteps ? `
                        <div
                            class="${completed ? 'bg-success' : 'bg-light'}"
                            style="
                                height:4px;
                                flex:1;
                                margin:0 12px;
                                border-radius:5px;
                            ">
                        </div>
                    ` : ""}

                </div>
            `;

        }

        html += `
                </div>
            </div>
        </div>
        `;

        return html;

    }

};