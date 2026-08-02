window.DeliveryStep={

render(){

return`

<div class="card shadow-sm">

<div class="card-header">

<h4>

Vehicle Delivery

</h4>

</div>

<div class="card-body">

<div class="alert alert-success">

Vehicle Ready For Delivery

</div>

<div class="d-flex justify-content-between">

<button
id="previousBtn"
class="btn btn-outline-secondary">

Previous

</button>

<div>

<button
id="deliveryBtn"
class="btn btn-primary">

Deliver Vehicle

</button>

<button
id="closeBtn"
class="btn btn-success">

Close Job

</button>

</div>

</div>

</div>

</div>

`;

},

bindEvents(){

document
.getElementById("deliveryBtn")
?.addEventListener(
"click",
async()=>{

await WorkflowService.readyForDelivery();

alert("Vehicle Delivered.");

});

document
.getElementById("closeBtn")
?.addEventListener(
"click",
async()=>{

await WorkflowService.closeJob();

alert("Job Closed Successfully.");

});

}

};