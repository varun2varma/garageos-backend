window.AssignTechnicianStep = {

    technicians: [],

    render() {

        return `

<div class="card shadow-sm">

<div class="card-header">

<h4>

Assign Technician

</h4>

</div>

<div class="card-body">

<div class="mb-3">

<label class="form-label">

Technician

</label>

<select
class="form-select"
id="technician">

<option value="">

Select Technician

</option>

</select>

</div>

<div
id="estimateServices">

Loading...

</div>

<div class="text-end mt-4">

<button
class="btn btn-primary"
id="assignTechnician">

Assign Jobs

</button>

</div>

</div>

</div>

`;

    },

    async bindEvents(){

        await this.loadTechnicians();

        await this.loadEstimateItems();

        document
            .getElementById(
                "assignTechnician"
            )
            .onclick=()=>{

                this.assign();

            };

    },

    async loadTechnicians(){

        const technicians=

            await UserService.technicians();

        const select=

            document.getElementById(
                "technician"
            );

        technicians.forEach(t=>{

            select.innerHTML+=`

            <option value="${t.id}">

            ${t.firstName} ${t.lastName}

            </option>

            `;

        });

    }

};