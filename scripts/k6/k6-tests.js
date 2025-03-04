import http from 'k6/http';
import { check, group } from 'k6';


export const options = {
    vus: 5,
    iterations: 100,
    duration: '60s',
};


const ENDPOINTS=[
    "non-async/clean",
    "non-async/cf-get",
    "async/cf-clean",
    "async/cf",
    "async/cf-with-async",
    "reactive/clean",
    "reactive/cf-get",
    "reactive/cf",
    "reactive/cf-with-async"
];
  
const URL = "http://localhost:8080/";

export default function () {
    for (let i = 0; i < ENDPOINTS.length; i++) {
        group(ENDPOINTS[i], function () {
            let res = http.get(URL+ENDPOINTS[i], {
                tags: {
                    name: ENDPOINTS[i]
                }
            });
            check(res, { "status is 200": (res) => res.status === 200 })
        });
    }
}