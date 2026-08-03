window.StatusBadge={

    render(status){

        const colors={

            OPEN:"warning",

            INSPECTION:"primary",

            ESTIMATE:"info",

            REPAIR:"secondary",

            QUALITY:"dark",

            PAYMENT:"success",

            DELIVERED:"success"

        };

        return `

<span class="badge bg-${colors[status]||"secondary"}">

    ${status.replaceAll("_"," ")}

</span>

`;

    }

};