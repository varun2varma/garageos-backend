window.ActiveJobs = {

    jobs: [],

    filteredJobs: [],

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

<i class="bi bi-list-task me-2"></i>

Active Jobs

</h2>

<p class="text-secondary mb-0">

Manage all ongoing service jobs

</p>

</div>

<div>

<button
class="btn btn-outline-primary"
id="refreshJobs">

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

id="jobSearch"

placeholder="Search Job Card / Customer / Vehicle">

</div>

</div>

<div class="col-md-7 text-end">

<span
class="badge bg-primary fs-6"
id="jobCountBadge">

0 Jobs

</span>

</div>

</div>

<div class="table-responsive">

<table
class="table table-hover align-middle">

<thead class="table-light">

<tr>

<th width="170">

Job Card

</th>

<th>

Customer

</th>

<th>

Vehicle

</th>

<th width="180">

Status

</th>

<th width="130">

Action

</th>

</tr>

</thead>

<tbody id="activeJobsTable">

<tr>

<td colspan="5"
class="text-center py-5">

<div
class="spinner-border spinner-border-sm">

</div>

Loading...

</td>

</tr>

</tbody>

</table>

</div>

<div
class="d-flex justify-content-between align-items-center mt-3">

<button
class="btn btn-outline-secondary"
id="prevPage">

<i class="bi bi-chevron-left"></i>

Previous

</button>

<div
class="fw-semibold"
id="pageInfo">

Page 1

</div>

<button
class="btn btn-outline-secondary"
id="nextPage">

Next

<i class="bi bi-chevron-right"></i>

</button>

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
                await JobCardService.getAll(
                    this.page,
                    this.size
                );

            if (response.content) {

                this.jobs = response.content;

                this.totalPages =
                    response.totalPages ?? 1;

                this.totalElements =
                    response.totalElements ??
                    this.jobs.length;

            } else {

                this.jobs =
                    response ?? [];

                this.totalPages = 1;

                this.totalElements =
                    this.jobs.length;

            }

            this.filteredJobs =
                [...this.jobs];

            this.renderTable();

        }

        catch (error) {

            console.error(error);

            if (window.Toast) {

                Toast.error(
                    "Unable to load jobs."
                );

            }

        }

        finally {

            this.loading = false;

        }

    },

    renderTable(list = this.filteredJobs) {

        const tbody =
            document.getElementById(
                "activeJobsTable"
            );

        if (!tbody) {

            return;

        }

        tbody.innerHTML = "";

        const badge =
            document.getElementById(
                "jobCountBadge"
            );

        if (badge) {

            badge.innerHTML =
                `${this.totalElements} Jobs`;

        }

        const pageInfo =
            document.getElementById(
                "pageInfo"
            );

        if (pageInfo) {

            pageInfo.innerHTML =
                `Page ${this.page + 1} of ${this.totalPages}`;

        }

        if (!list || list.length === 0) {

            tbody.innerHTML = `

<tr>

<td colspan="5"
class="text-center py-5 text-secondary">

No Active Jobs Found

</td>

</tr>

`;

            return;

        }

        list.forEach(job => {

            tbody.innerHTML += `

            <tr>

                <td>

                    <strong>

                        ${job.jobCardNumber ?? "-"}

                    </strong>

                </td>

                <td>

                    ${job.customerName ?? "-"}

                </td>

                <td>

                    ${job.vehicleName ?? "-"}

                </td>

                <td>

                    <span class="badge ${this.getStatusClass(job.status)}">

                        ${Dashboard.formatStatus(job.status)}

                    </span>

                </td>

                <td>

                    <button

                        class="btn btn-sm btn-primary continueJob"

                        data-job="${job.jobCardNumber}"

                        title="Continue Workflow">

                        Continue

                    </button>

                </td>

            </tr>

            `;

        });

    },

    bindEvents() {

        this.loadData();

        const refreshBtn =
            document.getElementById("refreshJobs");

        if (refreshBtn) {

            refreshBtn.onclick = () => {

                this.loadData();

            };

        }

        const searchBox =
            document.getElementById("jobSearch");

        if (searchBox) {

            searchBox.onkeyup = (e) => {

                const value =
                    e.target.value
                        .trim()
                        .toLowerCase();

                if (!value) {

                    this.filteredJobs =
                        [...this.jobs];

                    this.renderTable();

                    return;

                }

                this.filteredJobs =
                    this.jobs.filter(job => {

                        return (

                            (job.jobCardNumber ?? "")
                                .toLowerCase()
                                .includes(value)

                            ||

                            (job.customerName ?? "")
                                .toLowerCase()
                                .includes(value)

                            ||

                            (job.vehicleName ?? "")
                                .toLowerCase()
                                .includes(value)

                        );

                    });

                this.renderTable();

            };

        }

        const prevPage =
            document.getElementById("prevPage");

        if (prevPage) {

            prevPage.onclick = () => {

                if (this.page === 0) {

                    return;

                }

                this.page--;

                this.loadData();

            };

        }

        const nextPage =
            document.getElementById("nextPage");

        if (nextPage) {

            nextPage.onclick = () => {

                if (this.page >= this.totalPages - 1) {

                    return;

                }

                this.page++;

                this.loadData();

            };

        }

        document.onclick = async (e) => {

            const btn =
                e.target.closest(".continueJob");

            if (!btn) {

                return;

            }

            try {

                WorkflowHelper.reset();

                WorkflowHelper.state.jobCardNumber =
                    btn.dataset.job;

                Router.navigate("workflow");

            }

            catch (error) {

                console.error(error);

                if (window.Toast) {

                    Toast.error(
                        "Unable to continue workflow."
                    );

                }

            }

        };

    },

    getStatusClass(status) {

        switch (status) {

            case "CREATED":

                return "bg-secondary";

            case "INSPECTION_STARTED":

                return "bg-warning text-dark";

            case "INSPECTION_COMPLETED":

                return "bg-info text-dark";

            case "ESTIMATE_PREPARED":

                return "bg-primary";

                        case "ESTIMATE_APPROVED":

                            return "bg-success";

                        case "REPAIR_STARTED":

                            return "bg-warning text-dark";

                        case "REPAIR_COMPLETED":

                            return "bg-primary";

                        case "QUALITY_CHECK":

                            return "bg-dark";

                        case "QUALITY_CHECK_COMPLETED":

                            return "bg-info";

                        case "INVOICE_GENERATED":

                            return "bg-primary";

                        case "PAYMENT_PENDING":

                            return "bg-warning text-dark";

                        case "PAYMENT_COMPLETED":

                            return "bg-success";

                        case "READY_FOR_DELIVERY":

                            return "bg-success";

                        case "DELIVERED":

                            return "bg-success";

                        case "CLOSED":

                            return "bg-secondary";

                        case "CANCELLED":

                            return "bg-danger";

                        default:

                            return "bg-light text-dark";

                    }

                }

            };