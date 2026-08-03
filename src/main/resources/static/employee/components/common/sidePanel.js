window.SidePanel = {

    open(title, body) {

        document.getElementById("side-panel-container").innerHTML = `

            <div class="side-panel-backdrop">

                <div class="side-panel">

                    <div class="side-panel-header">

                        <h5>${title}</h5>

                        <button
                            id="closeSidePanel"
                            type="button">

                            <i class="bi bi-x-lg"></i>

                        </button>

                    </div>

                    <div class="side-panel-body">

                        ${body}

                    </div>

                </div>

            </div>

        `;

        document
            .getElementById("closeSidePanel")
            .addEventListener(
                "click",
                () => this.close()
            );

    },

    close() {

        document
            .getElementById("side-panel-container")
            .innerHTML = "";

    }

};