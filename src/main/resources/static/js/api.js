window.Api = {

    BASE_URL: "/api/v1",

    async get(url) {

        const response = await fetch(this.BASE_URL + url);

        if (!response.ok) {

            const error = new Error(await response.text());

            error.status = response.status;

            throw error;

        }

        const result = await response.json();
        return result.data ?? result;

//        return await response.json();

    },

    async post(url, body) {

        const response = await fetch(this.BASE_URL + url, {

            method: "POST",

            headers: {

                "Content-Type": "application/json"

            },

            body: JSON.stringify(body)

        });

        if (!response.ok) {

            const error = new Error(await response.text());

            error.status = response.status;

            throw error;

        }

//        return await response.json();
        const result = await response.json();
        return result.data ?? result;

    },

    async put(url, body) {

        const response = await fetch(this.BASE_URL + url, {

            method: "PUT",

            headers: {

                "Content-Type": "application/json"

            },

            body: JSON.stringify(body)

        });

        if (!response.ok) {

            const error = new Error(await response.text());

            error.status = response.status;

            throw error;

        }

//        return await response.json();
        const result = await response.json();
        return result.data ?? result;

    },


    async delete(url) {

        const response = await fetch(this.BASE_URL + url, {

            method: "DELETE"

        });

        if (!response.ok) {

            const error = new Error(await response.text());

            error.status = response.status;

            throw error;

        }

        const result = await response.json();

        return result.data ?? result;

    }

};