import React from "react";
import CoursesForm from "main/components/Courses/CoursesForm";
import { coursesFixtures } from "fixtures/coursesFixtures";
import { schoolsFixtures } from "fixtures/schoolsFixtures";
import { http, HttpResponse } from "msw";

export default {
  title: "components/Courses/CoursesForm",
  component: CoursesForm,
};

const Template = (args) => {
  return <CoursesForm {...args} />;
};

export const Create = Template.bind({});

Create.parameters = {
  msw: [
    http.get("/api/course/schools", () => {
      return HttpResponse.json(schoolsFixtures.activeSchools, {
        status: 200,
      });
    }),
  ],
};

Create.args = {
  buttonLabel: "Create",
  submitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};

export const Update = Template.bind({});

Update.parameters = Create.parameters;

Update.args = {
  initialContents: coursesFixtures.oneCourse[0],
  buttonLabel: "Update",
  submitAction: (data) => {
    console.log("Submit was clicked with data: ", data);
    window.alert("Submit was clicked with data: " + JSON.stringify(data));
  },
};
