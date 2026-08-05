window.TechnicianDashboard = {

    render() {

        return `

<div class="fade-in">

<div class="container-fluid">

<div class="mb-4">

<h2 class="fw-bold">

Good Morning 👋

</h2>

<p class="text-secondary">

Ready to start today's work?

</p>

</div>

<div class="row g-3 mb-4">

<div class="col-md-3">

<div class="card shadow-sm">

<div class="card-body">

<small>

Assigned Jobs

</small>

<h2 id="assignedJobs">

0

</h2>

</div>

</div>

</div>

<div class="col-md-3">

<div class="card shadow-sm">

<div class="card-body">

<small>

In Progress

</small>

<h2 id="inProgressJobs">

0

</h2>

</div>

</div>

</div>

<div class="col-md-3">

<div class="card shadow-sm">

<div class="card-body">

<small>

Completed Today

</small>

<h2 id="completedJobs">

0

</h2>

</div>

</div>

</div>

<div class="col-md-3">

<div class="card shadow-sm">

<div class="card-body">

<small>

Pending QC

</small>

<h2 id="pendingQc">

0

</h2>

</div>

</div>

</div>

</div>

<div class="card shadow-sm">

<div class="card-header">

Today's Assigned Jobs

</div>

<div
class="card-body"
id="technicianAssignments">

Loading...

</div>

</div>

</div>

</div>

`;

    },

    bindEvents() {

        this.loadAssignments();

        document.addEventListener("click", async (e) => {

            /*
            ---------------------------------------
            Accept Job
            ---------------------------------------
            */

            const acceptBtn = e.target.closest(".acceptJob");

            if (acceptBtn) {

                const assignmentId =
                    Number(acceptBtn.dataset.id);

                try {

                    await JobAssignmentService.accept(
                        assignmentId
                    );

                    await this.loadAssignments();

                    return;

                } catch (error) {

                    console.error(error);

                    alert(
                        error.message ||
                        "Unable to accept job."
                    );

                    return;

                }

            }

            /*
            ---------------------------------------
            Start Job
            ---------------------------------------
            */

            const startBtn = e.target.closest(".startJob");

            if (startBtn) {

                const assignmentId =
                    Number(startBtn.dataset.id);

                try {

                    await JobAssignmentService.start(
                        assignmentId,
                        {
                            remarks: ""
                        }
                    );

                    await this.loadAssignments();

                    return;

                } catch (error) {

                    console.error(error);

                    alert(
                        error.message ||
                        "Unable to start job."
                    );

                    return;

                }

            }

            /*
            ---------------------------------------
            Complete Job
            ---------------------------------------
            */

            const completeBtn =
                e.target.closest(".completeJob");

            if (completeBtn) {

                const assignmentId =
                    Number(completeBtn.dataset.id);

                try {

                    await JobAssignmentService.complete(
                        assignmentId,
                        {
                            actualHours: 1,
                            remarks: ""
                        }
                    );

                    await this.loadAssignments();

                    return;

                } catch (error) {

                    console.error(error);

                    alert(
                        error.message ||
                        "Unable to complete job."
                    );

                }

            }

        });

    },

    async loadAssignments(){

        const jobs =
            await JobAssignmentService.myAssignments();

        document.getElementById("assignedJobs").innerHTML =
            jobs.filter(x => x.status === "ASSIGNED").length;

        document.getElementById("inProgressJobs").innerHTML =
            jobs.filter(x => x.status === "IN_PROGRESS").length;

        document.getElementById("completedJobs").innerHTML =
            jobs.filter(x => x.status === "COMPLETED").length;

        document.getElementById("pendingQc").innerHTML =
            jobs.filter(x => x.status === "COMPLETED").length;

        this.renderAssignments(jobs);

    },

    renderAssignments(jobs){

        const div =
            document.getElementById(
                "technicianAssignments"
            );

        if(jobs.length===0){

            div.innerHTML=`

<div class="text-center py-5">

<h5>

No Jobs Assigned 🎉

</h5>

</div>

`;

            return;

        }

        div.innerHTML="";

        jobs.forEach(job=>{

            div.innerHTML+=`

<div class="card mb-3">

<div class="card-body">

<div class="row">

<div class="col-md-8">

<h5>

${job.jobCardNumber}

</h5>

<div>

${job.vehicleName}

</div>

<small>

${job.serviceName}

</small>

</div>

<div class="col-md-4 text-end">

${this.renderButton(job)}

</div>

</div>

</div>

</div>

`;

        });

    },

    renderButton(job){

        switch(job.status){

            case "ASSIGNED":

                return `

<button
class="btn btn-primary acceptJob"
data-id="${job.assignmentId}">

Accept Job

</button>

`;

            case "ACCEPTED":

                return `

<button
class="btn btn-warning startJob"
data-id="${job.assignmentId}">

Start Work

</button>

`;

            case "IN_PROGRESS":

                return `

<button
class="btn btn-success completeJob"
data-id="${job.assignmentId}">

Complete

</button>

`;

            default:

                return `
<span class="badge bg-success">

Completed

</span>
`;

        }

    }

};