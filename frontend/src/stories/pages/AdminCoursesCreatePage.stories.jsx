import React from "react";

import AdminCoursesCreatePage from "main/pages/AdminCoursesCreatePage";
import { schoolsFixtures } from "fixtures/schoolsFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "pages/AdminCoursesCreatePage",
  component: AdminCoursesCreatePage,
};

const Template = () => <AdminCoursesCreatePage />;

export const Default = Template.bind({});
Default.parameters = {
  msw: [
    http.get("/api/course/schools", () => {
      return HttpResponse.json(schoolsFixtures.activeSchools, {
        status: 200,
      });
    }),
  ],
};
