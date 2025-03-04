import http from 'k6/http';
import { check, group } from 'k6';

export const options = {
    vus: 2,
    iterations: 400,
    duration: '60s',
};

const URL = "http://localhost:8080/";

export default function() {

    let ENDPOINT=__ENV.ENDPOINT;

    group(ENDPOINT, function () {
        let res = http.get(URL+ENDPOINT, {
            tags: {
                name: ENDPOINT
            }
        });
        check(res, { "status is 200": (res) => res.status === 200 })
    });
}