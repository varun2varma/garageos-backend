window.Vehicles = {

    vehicles: [],

    filteredVehicles: [],

    page: 0,

    size: 10,

    totalPages: 0,

    totalElements: 0,

    loading: false,

    render() {

        return `

<div class="fade-in">

<div class="container-fluid">

<div class="d-flex justify-content-between align-items-center mb-4">

<div>

<h2 class="fw-bold mb-1">

<i class="bi bi-car-front-fill me-2"></i>

Vehicles

</h2>

<p class="text-secondary mb-0">

Manage registered vehicles

</p>

</div>

<div>

<button

class="btn btn-outline-primary"

id="refreshVehicles">

<i class="bi bi-arrow-clockwise me-1"></i>

Refresh

</button>

</div>

</div>

<div class="card shadow-sm border-0">

<div class="card-body">

<div class="row mb-3">

<div class="col-md-5">

<div class="input-group">

<span class="input-group-text">

<i class="bi bi-search"></i>

</span>

<input

type="text"

class="form-control"

id="vehicleSearch"

placeholder="Search Registration / Brand / Owner">

</div>

</div>

<div class="col-md-7 text-end">

<span

class="badge bg-primary fs-6"

id="vehicleCountBadge">

0 Vehicles

</span>

</div>

</div>

<div class="table-responsive">

<table

class="table table-hover align-middle">

<thead class="table-light">

<tr>

<th width="420">

Vehicle

</th>

<th>

Owner

</th>

<th width="130">

Action

</th>

</tr>

</thead>

<tbody id="vehicleTable">

<tr>

<td colspan="3"

class="text-center py-5">

<div

class="spinner-border spinner-border-sm">

</div>

Loading...

</td>

</tr>

</tbody>

</table>

<div

class="d-flex justify-content-between align-items-center mt-3">

<button

class="btn btn-outline-secondary"

id="prevVehiclePage">

<i class="bi bi-chevron-left"></i>

Previous

</button>

<div

class="fw-semibold"

id="vehiclePageInfo">

Page 1

</div>

<button

class="btn btn-outline-secondary"

id="nextVehiclePage">

Next

<i class="bi bi-chevron-right"></i>

</button>

</div>

</div>

</div>

</div>

</div>

`;

    },

    async loadData() {

        this.loading = true;

        try {

            const response =
                await VehicleService.getAll(
                    this.page,
                    this.size
                );

            if (response.content) {

                this.vehicles =
                    response.content;

                this.totalPages =
                    response.totalPages ?? 1;

                this.totalElements =
                    response.totalElements ??
                    this.vehicles.length;

            }

            else {

                this.vehicles =
                    response ?? [];

                this.totalPages = 1;

                this.totalElements =
                    this.vehicles.length;

            }

            this.filteredVehicles =
                [...this.vehicles];

            this.renderTable();

        }

        catch (e) {

            console.error(e);

            if (window.Toast) {

                Toast.error(
                    "Unable to load vehicles."
                );

            }

        }

        finally {

            this.loading = false;

        }

    },

    renderTable(list = this.filteredVehicles) {

        const tbody =
            document.getElementById(
                "vehicleTable"
            );

        if (!tbody) {

            return;

        }

        tbody.innerHTML = "";

        const badge =
            document.getElementById(
                "vehicleCountBadge"
            );

        if (badge) {

            badge.innerHTML =
                `${this.totalElements} Vehicles`;

        }

        const pageInfo =
            document.getElementById(
                "vehiclePageInfo"
            );

        if (pageInfo) {

            pageInfo.innerHTML =
                `Page ${this.page + 1} of ${this.totalPages}`;

        }

        if (!list.length) {

            tbody.innerHTML = `

<tr>

<td colspan="3"

class="text-center py-5 text-secondary">

No Vehicles Found

</td>

</tr>

`;

            return;

        }

        list.forEach(vehicle => {

            tbody.innerHTML += `

            <tr>

                <td>

                    <div class="d-flex align-items-start">

                        <div
                            class="rounded-circle bg-primary text-white d-flex align-items-center justify-content-center me-3"
                            style="width:42px;height:42px;min-width:42px;">

                            <i class="bi bi-car-front-fill"></i>

                        </div>

                        <div>

                            <div class="fw-bold">

                                ${vehicle.registrationNumber ?? "-"}

                            </div>

                            <small class="text-secondary">

                                ${vehicle.brand ?? ""}

                                ${vehicle.model ?? ""}

                                ${vehicle.variant ?? ""}

                            </small>

                        </div>

                    </div>

                </td>

                <td>

                    <div class="fw-semibold">

                        ${vehicle.customerName ?? "-"}

                    </div>

                    <small class="text-secondary">

                        ${vehicle.customerMobileNumber ?? "-"}

                    </small>

                </td>

                <td>

                    <button

                        class="btn btn-primary btn-sm viewVehicle"

                        data-id="${vehicle.id}">

                        <i class="bi bi-eye"></i>

                        View

                    </button>

                </td>

            </tr>

            `;

        });

    },

    bindEvents() {

        this.loadData();

        const refreshButton =
            document.getElementById(
                "refreshVehicles"
            );

        if (refreshButton) {

            refreshButton.onclick = () => {

                this.loadData();

            };

        }

        const searchBox =
            document.getElementById(
                "vehicleSearch"
            );

        if (searchBox) {

            searchBox.onkeyup = (e) => {

                const value =
                    e.target.value
                        .trim()
                        .toLowerCase();

                if (!value) {

                    this.filteredVehicles =
                        [...this.vehicles];

                    this.renderTable();

                    return;

                }

                this.filteredVehicles =
                    this.vehicles.filter(vehicle => {

                        return (

                            (vehicle.registrationNumber ?? "")
                                .toLowerCase()
                                .includes(value)

                            ||

                            (vehicle.brand ?? "")
                                .toLowerCase()
                                .includes(value)

                            ||

                            (vehicle.model ?? "")
                                .toLowerCase()
                                .includes(value)

                            ||

                            (vehicle.variant ?? "")
                                .toLowerCase()
                                .includes(value)

                            ||

                            (vehicle.customerName ?? "")
                                .toLowerCase()
                                .includes(value)

                            ||

                            (vehicle.customerMobileNumber ?? "")
                                .toLowerCase()
                                .includes(value)

                        );

                    });

                this.renderTable();

            };

        }

        const prevButton =
            document.getElementById(
                "prevVehiclePage"
            );

        if (prevButton) {

            prevButton.onclick = () => {

                if (this.page === 0) {

                    return;

                }

                this.page--;

                this.loadData();

            };

        }

        const nextButton =
            document.getElementById(
                "nextVehiclePage"
            );

        if (nextButton) {

            nextButton.onclick = () => {

                if (this.page >= this.totalPages - 1) {

                    return;

                }

                this.page++;

                this.loadData();

            };

        }

        document.onclick = (e) => {

            const btn =
                e.target.closest(".viewVehicle");

            if (!btn) {

                return;

            }

            console.log(
                "Vehicle Id :",
                btn.dataset.id
            );

        };

    }
};