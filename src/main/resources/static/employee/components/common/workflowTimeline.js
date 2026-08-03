window.WorkflowTimeline = {

    STEPS: [

        {
            key: "CUSTOMER",
            title: "Customer"
        },

        {
            key: "VEHICLE",
            title: "Vehicle"
        },

        {
            key: "JOB_CARD",
            title: "Job Card"
        },

        {
            key: "INSPECTION",
            title: "Inspection"
        },

        {
            key: "ESTIMATE",
            title: "Estimate"
        },

        {
            key: "APPROVAL",
            title: "Approval"
        },

        {
            key: "REPAIR",
            title: "Repair"
        },

        {
            key: "QUALITY",
            title: "Quality"
        },

        {
            key: "INVOICE",
            title: "Invoice"
        },

        {
            key: "PAYMENT",
            title: "Payment"
        },

        {
            key: "DELIVERY",
            title: "Delivery"
        }

    ],

    render(currentStep) {

        let html = `

<div class="workflow-timeline">

`;

        this.STEPS.forEach((step, index) => {

            const completed =
                index < currentStep;

            const active =
                index === currentStep;

            html += `

<div class="timeline-step">

    <div class="timeline-circle

        ${completed ? "completed" : ""}

        ${active ? "active" : ""}">

        ${
            completed
                ? '<i class="bi bi-check-lg"></i>'
                : active
                    ? '<i class="bi bi-hourglass-split"></i>'
                    : ""
        }

    </div>

    <div class="timeline-title">

        ${step.title}

    </div>

</div>

`;

        });

        html += `

</div>

`;

        return html;

    }

};