window.Customers = {

    customers: [],

    filteredCustomers: [],

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

<i class="bi bi-people-fill me-2"></i>

Customers

</h2>

<p class="text-secondary mb-0">

Manage all garage customers

</p>

</div>

<div>

<button

class="btn btn-outline-primary"

id="refreshCustomers">

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

id="customerSearch"

placeholder="Search Name / Mobile / Email">

</div>

</div>

<div class="col-md-7 text-end">

<span

class="badge bg-primary fs-6"

id="customerCountBadge">

0 Customers

</span>

</div>

</div>

<div class="table-responsive">

<table

class="table table-hover align-middle">

<thead class="table-light">

<tr>

<th width="70">

Customer

</th>

<th>

Name

</th>

<th>

Mobile

</th>

<th>

Email

</th>

<th width="120">

Action

</th>

</tr>

</thead>

<tbody id="customerTable">

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

<div

class="d-flex justify-content-between align-items-center mt-3">

<button

class="btn btn-outline-secondary"

id="prevCustomerPage">

<i class="bi bi-chevron-left"></i>

Previous

</button>

<div

class="fw-semibold"

id="customerPageInfo">

Page 1

</div>

<button

class="btn btn-outline-secondary"

id="nextCustomerPage">

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
                await CustomerService.getAll(
                    this.page,
                    this.size
                );

            if (response.content) {

                this.customers =
                    response.content;

                this.totalPages =
                    response.totalPages ?? 1;

                this.totalElements =
                    response.totalElements ??
                    this.customers.length;

            }

            else {

                this.customers =
                    response ?? [];

                this.totalPages = 1;

                this.totalElements =
                    this.customers.length;

            }

            this.filteredCustomers =
                [...this.customers];

            this.renderTable();

        }

        catch (e) {

            console.error(e);

            if (window.Toast) {

                Toast.error(
                    "Unable to load customers."
                );

            }

        }

        finally {

            this.loading = false;

        }

    },

    renderTable(list = this.filteredCustomers) {

        const tbody =
            document.getElementById(
                "customerTable"
            );

        if (!tbody) {

            return;

        }

        tbody.innerHTML = "";

        const badge =
            document.getElementById(
                "customerCountBadge"
            );

        if (badge) {

            badge.innerHTML =
                `${this.totalElements} Customers`;

        }

        const pageInfo =
            document.getElementById(
                "customerPageInfo"
            );

        if (pageInfo) {

            pageInfo.innerHTML =
                `Page ${this.page + 1} of ${this.totalPages}`;

        }

        if (!list.length) {

            tbody.innerHTML = `

<tr>

<td colspan="5"

class="text-center py-5 text-secondary">

No Customers Found

</td>

</tr>

`;

            return;

        }

        list.forEach(customer => {

            const fullName =
                `${customer.firstName ?? ""} ${customer.lastName ?? ""}`.trim();

            const letter =
                (fullName || "C")
                    .charAt(0)
                    .toUpperCase();

            tbody.innerHTML += `

            <tr>

                <td>

                    <div

                        class="rounded-circle bg-primary text-white d-flex align-items-center justify-content-center"

                        style="width:40px;height:40px;font-weight:600;">

                        ${letter}

                    </div>

                </td>

                <td>

                    <strong>

                        ${fullName || "-"}

                    </strong>

                </td>

                <td>

                    ${customer.mobileNumber ?? "-"}

                </td>

                <td>

                    ${customer.email ?? "-"}

                </td>

                <td>

                    <button

                        class="btn btn-sm btn-primary viewCustomer"

                        data-id="${customer.id}">

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
                "refreshCustomers"
            );

        if (refreshButton) {

            refreshButton.onclick = () => {

                this.loadData();

            };

        }

        const searchBox =
            document.getElementById(
                "customerSearch"
            );

        if (searchBox) {

            searchBox.onkeyup = (e) => {

                const value =
                    e.target.value
                        .trim()
                        .toLowerCase();

                if (!value) {

                    this.filteredCustomers =
                        [...this.customers];

                    this.renderTable();

                    return;

                }

                this.filteredCustomers =
                    this.customers.filter(customer => {

                        return (

                            (
                                `${customer.firstName ?? ""} ${customer.lastName ?? ""}`
                            )
                            .toLowerCase()
                            .includes(value)

                            ||

                            (customer.mobileNumber ?? "")
                                .toLowerCase()
                                .includes(value)

                            ||

                            (customer.email ?? "")
                                .toLowerCase()
                                .includes(value)

                        );

                    });

                this.renderTable();

            };

        }

        const prevButton =
            document.getElementById(
                "prevCustomerPage"
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
                "nextCustomerPage"
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
                e.target.closest(".viewCustomer");

            if (!btn) {

                return;

            }

            console.log(
                "Customer Id :",
                btn.dataset.id
            );

        };

    }

};