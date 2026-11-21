import React from "react";
import { Route, Routes } from "react-router";
import { http, HttpResponse } from "msw";
import PagedProfitsTable from "main/components/Game/PagedProfitsTable";
import pagedProfitsFixtures from "fixtures/pagedProfitsFixtures";

export default {
  title: "components/Game/PagedProfitsTable",
  component: PagedProfitsTable,
};

const makeTemplate = (gameId, pageNumber = 0) => {
  function Template() {
    const search = new URLSearchParams({ pageNumber: pageNumber.toString() });
    return (
      <Routes location={`/play/${gameId}?${search.toString()}`}>
        <Route path="/play/:gameId" element={<PagedProfitsTable />} />
      </Routes>
    );
  }
  return Template;
};

export const FirstPage = makeTemplate(1, 0);

FirstPage.parameters = {
  msw: [
    http.get("/api/profits/paged/gameid", ({ request }) => {
      const url = new URL(request.url);
      const params = Object.fromEntries(url.searchParams.entries());
      if (params.gameId === "1") {
        const pageNumber = parseInt(params.pageNumber, 10);
        if (pageNumber === 0) {
          return HttpResponse.json(pagedProfitsFixtures.twoPages[0], {
            status: 200,
          });
        }
        if (pageNumber === 1) {
          return HttpResponse.json(pagedProfitsFixtures.twoPages[1], {
            status: 200,
          });
        }
        return HttpResponse.json(pagedProfitsFixtures.emptyPage, {
          status: 200,
        });
      }
      return HttpResponse.json(pagedProfitsFixtures.emptyPage, { status: 200 });
    }),
  ],
};

export const SecondPage = makeTemplate(1, 1);

SecondPage.parameters = {
  msw: [
    http.get("/api/profits/paged/gameid", ({ request }) => {
      const url = new URL(request.url);
      const params = Object.fromEntries(url.searchParams.entries());
      if (params.gameId === "1") {
        const pageNumber = parseInt(params.pageNumber, 10);
        if (pageNumber === 0) {
          return HttpResponse.json(pagedProfitsFixtures.twoPages[0], {
            status: 200,
          });
        }
        if (pageNumber === 1) {
          return HttpResponse.json(pagedProfitsFixtures.twoPages[1], {
            status: 200,
          });
        }
        return HttpResponse.json(pagedProfitsFixtures.emptyPage, {
          status: 200,
        });
      }
      return HttpResponse.json(pagedProfitsFixtures.emptyPage, { status: 200 });
    }),
  ],
};

export const EmptyTable = makeTemplate(1, 0);

EmptyTable.parameters = {
  msw: [
    http.get("/api/profits/paged/gameid", () => {
      return HttpResponse.json(pagedProfitsFixtures.emptyPage, { status: 200 });
    }),
  ],
};
