window.OwnerEmployees = {

    pendingEmployees: [],

    employees: [],

    selectedMembershipId: null,

    approveModal: null,

    rejectModal: null,

    async init() {

        this.approveModal =
            new bootstrap.Modal(
                document.getElementById(
                    "approveEmployeeModal"
                )
            );

        this.rejectModal =
            new bootstrap.Modal(
                document.getElementById(
                    "rejectEmployeeModal"
                )
            );

        this.bindEvents();

        await this.refresh();

    },

    async refresh() {

        try {

            await Promise.all([

                this.loadPendingEmployees(),

                this.loadEmployees(),

                this.loadRoles()

            ]);

        }
        catch (e) {

            console.error(e);

        }

    },

    async loadPendingEmployees() {

        this.pendingEmployees =
            await OwnerService.getPendingEmployees();

        this.renderPendingEmployees();

    },

    async loadEmployees() {

        this.employees =
            await OwnerService.getEmployees();

        this.renderEmployees();

    },

    async loadRoles() {

        const roles =
            await MasterService.getEmployeeRoles();

        const select =
            document.getElementById(
                "employeeRoles"
            );

        if (!select) {

            return;

        }

        select.innerHTML = "";

        roles.forEach(role => {

            select.innerHTML += `

                <option
                    value="${role.id}">

                    ${role.displayName}

                </option>

            `;

        });

    },

    bindEvents() {

        const search =
            document.getElementById(
                "employeeSearch"
            );

        if (search) {

            search.addEventListener(

                "keyup",

                e =>

                    this.searchEmployees(
                        e.target.value
                    )

            );

        }

        document

            .getElementById(
                "approveEmployeeBtn"
            )

            .addEventListener(

                "click",

                () => this.approveEmployee()

            );

        document

            .getElementById(
                "rejectEmployeeBtn"
            )

            .addEventListener(

                "click",

                () => this.rejectEmployee()

            );

    },

    renderPendingEmployees() {

        const container =
            document.getElementById(
                "pendingEmployees"
            );

        const badge =
            document.getElementById(
                "pendingBadge"
            );

        const count =
            document.getElementById(
                "pendingCount"
            );

        if (badge) {

            badge.textContent =
                this.pendingEmployees.length;

        }

        if (count) {

            count.textContent =
                this.pendingEmployees.length;

        }

        if (!container) {

            return;

        }

        if (this.pendingEmployees.length === 0) {

            container.innerHTML = `

                <div class="text-center py-5">

                    <i
                        class="bi bi-check-circle-fill
                               text-success
                               fs-1">

                    </i>

                    <h5 class="mt-3">

                        No Pending Requests

                    </h5>

                    <p class="text-muted">

                        All employee requests have been processed.

                    </p>

                </div>

            `;

            return;

        }

        container.innerHTML =

            this.pendingEmployees

                .map(employee => `

                    <div class="card mb-3 border-0 shadow-sm">

                        <div class="card-body">

                            <div class="row align-items-center">

                                <div class="col-lg-8">

                                    <h5 class="mb-1">

                                        ${employee.firstName}
                                        ${employee.lastName}

                                    </h5>

                                    <div class="text-muted">

                                        <i class="bi bi-envelope"></i>

                                        ${employee.email}

                                    </div>

                                    <div class="text-muted">

                                        <i class="bi bi-phone"></i>

                                        ${employee.mobile}

                                    </div>

                                    <small
                                        class="badge bg-warning mt-2">

                                        Pending Approval

                                    </small>

                                </div>

                                <div
                                    class="col-lg-4
                                           text-end">

                                    <button

                                            class="btn btn-primary me-2"

                                            onclick="OwnerEmployees.openApproveModal(${employee.id})">

                                        <i class="bi bi-check-circle"></i>

                                        Approve

                                    </button>

                                    <button

                                            class="btn btn-outline-danger"

                                            onclick="OwnerEmployees.openRejectModal(${employee.id})">

                                        Reject

                                    </button>

                                </div>

                            </div>

                        </div>

                    </div>

                `)

                .join("");

    },

    renderEmployees() {

        const tbody =
            document.getElementById(
                "employees"
            );

        const badge =
            document.getElementById(
                "employeeBadge"
            );

        const count =
            document.getElementById(
                "employeeCount"
            );

        if (badge) {

            badge.textContent =
                this.employees.length;

        }

        if (count) {

            count.textContent =
                this.employees.length;

        }

        if (!tbody) {

            return;

        }

        if (this.employees.length === 0) {

            tbody.innerHTML = `

                <tr>

                    <td
                        colspan="6"
                        class="text-center py-5">

                        No employees found.

                    </td>

                </tr>

            `;

            return;

        }

        tbody.innerHTML =

            this.employees

                .map(employee => `

                    <tr>

                        <td>

                            <strong>

                                ${employee.firstName}
                                ${employee.lastName}

                            </strong>

                        </td>

                        <td>

                            ${employee.employeeCode ?? "-"}

                        </td>

                        <td>

                            <div>

                                ${employee.mobile}

                            </div>

                            <small class="text-muted">

                                ${employee.email}

                            </small>

                        </td>

                        <td>

                            ${(employee.roles ?? [])

                                .map(role => `

                                    <span
                                        class="badge bg-primary me-1">

                                        ${role}

                                    </span>

                                `)

                                .join("")}

                        </td>

                        <td>

                            <span
                                class="badge bg-success">

                                Active

                            </span>

                        </td>

                        <td>

                            <button

                                    class="btn btn-sm btn-outline-danger"

                                    onclick="OwnerEmployees.removeEmployee(${employee.id})">

                                Remove

                            </button>

                        </td>

                    </tr>

                `)

                .join("");

        this.updateStatistics();

    },

    updateStatistics() {

        const managers =
            this.employees.filter(

                employee =>

                    (employee.roles ?? [])

                        .includes("MANAGER")

            ).length;

        const technicians =
            this.employees.filter(

                employee =>

                    (employee.roles ?? [])

                        .includes("TECHNICIAN")

            ).length;

        document.getElementById(
            "managerCount"
        ).textContent =
            managers;

        document.getElementById(
            "technicianCount"
        ).textContent =
            technicians;

    },

    searchEmployees(keyword) {

        keyword =
            keyword.toLowerCase();

        const rows =
            document.querySelectorAll(
                "#employees tr"
            );

        rows.forEach(row => {

            row.style.display =
                row.innerText
                    .toLowerCase()
                    .includes(keyword)
                    ? ""
                    : "none";

        });

    },

    openApproveModal(membershipId) {

        this.selectedMembershipId =
            membershipId;

        document.getElementById(
            "employeeCode"
        ).value = "";

        const roleSelect =
            document.getElementById(
                "employeeRoles"
            );

        if (roleSelect) {

            Array.from(roleSelect.options)

                .forEach(option =>

                    option.selected = false

                );

        }

        this.approveModal.show();

    },

    async approveEmployee() {

        try {

            const employeeCode =
                document
                    .getElementById(
                        "employeeCode"
                    )
                    .value
                    .trim();

            if (!employeeCode) {

                alert(
                    "Employee Code is required."
                );

                return;

            }

            const roles =
                Array.from(

                    document
                        .getElementById(
                            "employeeRoles"
                        )
                        .selectedOptions

                ).map(option =>

                    Number(option.value)

                );

            if (!roles.length) {

                alert(
                    "Please select at least one role."
                );

                return;

            }

            await OwnerService.approveEmployee(

                this.selectedMembershipId,

                {

                    employeeCode,

                    roleIds: roles

                }

            );

            alert(
                "Employee approved successfully."
            );

            // Close modal
            bootstrap.Modal
                .getInstance(
                    document.getElementById("approveEmployeeModal")
                )
                ?.hide();

            // Reload everything
            await this.loadPendingEmployees();

            await this.loadEmployees();

            this.updateStatistics();

        } catch (e) {

            console.error(e);

            alert(

                e.message ??

                "Unable to approve employee."

            );

        }

    },

    openRejectModal(membershipId) {

        this.selectedMembershipId =
            membershipId;

        document.getElementById(
            "rejectRemarks"
        ).value = "";

        this.rejectModal.show();

    },

    async rejectEmployee() {

        try {

            const remarks =
                document
                    .getElementById(
                        "rejectRemarks"
                    )
                    .value
                    .trim();

            if (!remarks) {

                alert(
                    "Remarks are required."
                );

                return;

            }

            await OwnerService.rejectEmployee(

                this.selectedMembershipId,

                remarks

            );

            this.rejectModal.hide();

            alert(
                "Employee rejected successfully."
            );

            await this.refresh();

        } catch (e) {

            console.error(e);

            alert(

                e.message ??

                "Unable to reject employee."

            );

        }

    },

    async removeEmployee(membershipId) {

        try {

            if (

                !confirm(

                    "Remove this employee from your garage?"

                )

            ) {

                return;

            }

            await OwnerService.removeEmployee(

                membershipId

            );

            alert(
                "Employee removed successfully."
            );

            await this.refresh();

        } catch (e) {

            console.error(e);

            alert(

                e.message ??

                "Unable to remove employee."

            );

        }

    }

};