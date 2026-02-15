import api from "./index";

const canteenApi = {
  getAll() {
    return api.get("/api/canteens");
  },
};

export default canteenApi;
