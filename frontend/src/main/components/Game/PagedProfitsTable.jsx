import React from "react";
import OurTable from "main/components/OurTable";
import { Button } from "react-bootstrap";
import { useBackend } from "main/utils/useBackend";
import { useSearchParams, useParams } from "react-router";
import { formatPacificDate } from "main/utils/dateUtils";

const PagedProfitsTable = () => {
  const testId = "PagedProfitsTable";
  const refreshJobsIntervalMilliseconds = 5000;

  const [searchParams, setSearchParams] = useSearchParams();
  const selectedPage = parseInt(searchParams.get("pageNumber") || "0", 10);

  const PROFIT_PAGE_SIZE = 5;
  const { gameId, userId } = useParams();

  const isAdminView = userId !== undefined;

  const { data: page } = useBackend(
    [
      isAdminView ? "/api/profits/paged" : "/api/profits/paged/gameid",
      selectedPage,
    ],
    {
      method: "GET",
      url: isAdminView ? "/api/profits/paged" : "/api/profits/paged/gameid",
      params: {
        ...(isAdminView && { userId: userId }),
        gameId: gameId,
        pageNumber: selectedPage,
        pageSize: PROFIT_PAGE_SIZE,
      },
    },
    { content: [], totalPages: 0 },
    { refetchInterval: refreshJobsIntervalMilliseconds },
  );

  const testid = "PagedProfitsTable";

  const previousPageCallback = () => {
    return () => {
      setSearchParams({ pageNumber: (selectedPage - 1).toString() });
    };
  };

  const nextPageCallback = () => {
    return () => {
      setSearchParams({ pageNumber: (selectedPage + 1).toString() });
    };
  };

  const columns = [
    {
      Header: "Profit",
      accessor: "amount",
      Cell: ({ value }) => `$${value.toFixed(2)}`,
      width: 200,
    },
    {
      Header: "Date",
      accessor: "timestamp",
      Cell: ({ value }) => formatPacificDate(value),
      width: 200,
    },
    {
      Header: "Health",
      accessor: "avgCowHealth",
      Cell: ({ value }) => `${value.toFixed(2) + "%"}`,
      width: 200,
    },
    {
      Header: "Cows",
      accessor: "numCows",
      width: 200,
    },
  ];

  const sortees = React.useMemo(
    () => [
      {
        id: "timestamp",
        desc: true,
      },
    ],
    [],
  );

  return (
    <>
      <p>Page: {selectedPage + 1}</p>
      <Button
        data-testid={`${testId}-previous-button`}
        onClick={previousPageCallback()}
        disabled={selectedPage === 0}
      >
        Previous
      </Button>
      <Button
        data-testid={`${testId}-next-button`}
        onClick={nextPageCallback()}
        disabled={page.totalPages === 0 || selectedPage === page.totalPages - 1}
      >
        Next
      </Button>
      <div
        data-testid="PagedProfitsTable-CSS"
        style={{
          display: "flex",
          overflow: "auto",
        }}
      >
        <OurTable
          data={page.content}
          columns={columns}
          testid={testid}
          initialState={{ sortBy: sortees }}
        />
      </div>
    </>
  );
};

export default PagedProfitsTable;
