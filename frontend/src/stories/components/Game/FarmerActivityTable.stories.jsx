import React from "react";
import { http, HttpResponse } from "msw";
import FarmerActivityTable from "main/components/Game/FarmerActivityTable";
//import farmerFixtures from "fixtures/farmerFixtures";

export default {
  title: "components/Game/FarmerActivityTable",
  component: FarmerActivityTable,
};

const Template = (args) => {
  return <FarmerActivityTable {...args} />;
};

const singleFarmerActivity = [
  {
    id: 1,
    studentId: 42,
    timestamp: "2024-01-15T10:20:00",
    activityType: 1,
    numCows: 3,
  },
];

const gameWideActivity = [
  {
    id: 3,
    studentId: 42,
    timestamp: "2024-01-15T10:20:00",
    activityType: 1,
    numCows: 3,
    farmer: { userId: 2, gameId: 1, username: "farmerjoe" },
  },
  {
    id: 2,
    studentId: 43,
    timestamp: "2024-01-15T10:15:00",
    activityType: 2,
    numCows: 1,
    farmer: { userId: 5, gameId: 1, username: "farmerjane" },
  },
  {
    id: 1,
    studentId: 44,
    timestamp: "2024-01-15T10:00:00",
    activityType: 0,
    numCows: 0,
    farmer: { userId: 7, gameId: 1, username: "farmerbob" },
  },
];

export const ForSingleFarmer = Template.bind({});

ForSingleFarmer.args = {
  gameId: 1,
  userId: 42,
};

ForSingleFarmer.parameters = {
  msw: [
    http.get("/api/farmeractivity/all", ({ request }) => {
      const url = new URL(request.url);
      const params = Object.fromEntries(url.searchParams.entries());
      if (params.userId === "42" && params.gameId === "1") {
        return HttpResponse.json(singleFarmerActivity, { status: 200 });
      }
      return HttpResponse.json([], { status: 200 });
    }),
  ],
};

export const ForWholeGame = Template.bind({});

ForWholeGame.args = {
  gameId: 1,
  userId: undefined,
};

ForWholeGame.parameters = {
  msw: [
    http.get("/api/farmeractivity/game", ({ request }) => {
      const url = new URL(request.url);
      const params = Object.fromEntries(url.searchParams.entries());
      if (params.gameId === "1") {
        return HttpResponse.json(gameWideActivity, { status: 200 });
      }
      return HttpResponse.json([], { status: 200 });
    }),
  ],
};

export const EmptyTable = Template.bind({});

EmptyTable.args = {
  gameId: 1,
  userId: 42,
};

EmptyTable.parameters = {
  msw: [
    http.get("/api/farmeractivity/all", () => {
      return HttpResponse.json([], { status: 200 });
    }),
  ],
};
