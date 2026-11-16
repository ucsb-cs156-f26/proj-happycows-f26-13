import { fireEvent, render, screen, waitFor } from "@testing-library/react";
import { QueryClient, QueryClientProvider } from "react-query";
import { MemoryRouter } from "react-router";
import axios from "axios";
import AxiosMockAdapter from "axios-mock-adapter";
import { vi } from "vitest";
import * as backend from "main/utils/useBackend";
import AdminBannedWordsPage from "main/pages/AdminBannedWordsPage";
import { apiCurrentUserFixtures } from "fixtures/currentUserFixtures";
import { systemInfoFixtures } from "fixtures/systemInfoFixtures";

const mockToast = vi.hoisted(() => vi.fn());

vi.mock("react-toastify", async () => {
  const originalModule = await vi.importActual("react-toastify");
  const toast = (message) => mockToast(message);
  toast.error = (message) => mockToast(message);
  return { ...originalModule, toast };
});

describe("AdminBannedWordsPage tests", () => {
  const axiosMock = new AxiosMockAdapter(axios);
  const endpoint = "/api/admin/bannedwords/all";

  const renderPage = () => {
    const queryClient = new QueryClient({
      defaultOptions: { queries: { retry: false } },
    });

    return render(
      <QueryClientProvider client={queryClient}>
        <MemoryRouter>
          <AdminBannedWordsPage />
        </MemoryRouter>
      </QueryClientProvider>,
    );
  };

  beforeEach(() => {
    axiosMock.reset();
    axiosMock.resetHistory();
    mockToast.mockClear();

    axiosMock
      .onGet("/api/currentUser")
      .reply(200, apiCurrentUserFixtures.adminUser);
    axiosMock
      .onGet("/api/systemInfo")
      .reply(200, systemInfoFixtures.showingNeither);
  });

  afterAll(() => axiosMock.restore());

  test("uses the correct API configuration and query keys", () => {
    axiosMock.onGet(endpoint).reply(200, []);

    const querySpy = vi.spyOn(backend, "useBackend");
    const mutationSpy = vi.spyOn(backend, "useBackendMutation");

    try {
      renderPage();

      expect(querySpy).toHaveBeenCalledWith(
        [endpoint],
        { method: "GET", url: endpoint },
        [],
      );

      expect(mutationSpy).toHaveBeenCalledTimes(2);
      expect(mutationSpy.mock.calls.map((call) => call[2])).toEqual([
        [endpoint],
        [endpoint],
      ]);

      expect(screen.getByRole("textbox", { name: "Word" })).toHaveValue("");
    } finally {
      querySpy.mockRestore();
      mutationSpy.mockRestore();
    }
  });

  test("displays the banned word list", async () => {
    axiosMock.onGet(endpoint).reply(200, [
      { id: 1, word: "darn" },
      { id: 2, word: "heck" },
    ]);

    renderPage();

    expect(
      screen.getByRole("heading", { name: "Banned Words" }),
    ).toBeInTheDocument();
    expect(await screen.findByText("darn")).toBeInTheDocument();
    expect(screen.getByText("heck")).toBeInTheDocument();
    expect(screen.getAllByRole("button", { name: /^Delete / })).toHaveLength(2);
  });

  test("displays an empty list", async () => {
    axiosMock.onGet(endpoint).reply(200, []);

    renderPage();

    await waitFor(() => {
      expect(
        axiosMock.history.get.some((request) => request.url === endpoint),
      ).toBe(true);
    });

    expect(screen.getByRole("table")).toBeInTheDocument();
    expect(
      screen.queryByRole("button", { name: /^Delete / }),
    ).not.toBeInTheDocument();
  });

  test("adds a word, clears the input, and refreshes the list", async () => {
    let words = [{ id: 1, word: "darn" }];

    axiosMock.onGet(endpoint).reply(() => [200, words]);
    axiosMock.onPost("/api/admin/bannedwords/post").reply((request) => {
      words = [...words, { id: 2, word: request.params.word }];
      return [200, words[1]];
    });

    renderPage();

    expect(await screen.findByText("darn")).toBeInTheDocument();

    const input = screen.getByRole("textbox", { name: "Word" });
    fireEvent.change(input, { target: { value: "heck" } });
    fireEvent.click(screen.getByRole("button", { name: "Add" }));

    await waitFor(() => {
      expect(axiosMock.history.post).toHaveLength(1);
      expect(input).toHaveValue("");
    });

    expect(axiosMock.history.post[0].params).toEqual({ word: "heck" });
    expect(await screen.findByText("heck")).toBeInTheDocument();
  });

  test("deletes a word and refreshes the list", async () => {
    let words = [
      { id: 1, word: "darn" },
      { id: 2, word: "heck" },
    ];

    axiosMock.onGet(endpoint).reply(() => [200, words]);
    axiosMock.onDelete("/api/admin/bannedwords").reply((request) => {
      words = words.filter((entry) => entry.id !== request.params.id);
      return [200, "Deleted"];
    });

    renderPage();

    expect(await screen.findByText("darn")).toBeInTheDocument();
    fireEvent.click(screen.getByRole("button", { name: "Delete darn" }));

    await waitFor(() => {
      expect(axiosMock.history.delete).toHaveLength(1);
      expect(screen.queryByText("darn")).not.toBeInTheDocument();
    });

    expect(axiosMock.history.delete[0].params).toEqual({ id: 1 });
    expect(screen.getByText("heck")).toBeInTheDocument();
  });

  test.each([
    ["duplicate", "darn", "Word already exists"],
    ["spaces", "two words", "Word cannot contain spaces"],
    ["blank", "", "Word cannot be blank"],
  ])("shows the backend error for %s input", async (_case, value, message) => {
    axiosMock.onGet(endpoint).reply(200, []);
    axiosMock.onPost("/api/admin/bannedwords/post").reply(400, { message });

    renderPage();

    const input = screen.getByRole("textbox", { name: "Word" });
    fireEvent.change(input, { target: { value } });
    fireEvent.click(screen.getByRole("button", { name: "Add" }));

    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith(message);
    });

    expect(input).toHaveValue(value);
  });

  test("shows the backend error when deletion fails", async () => {
    axiosMock.onGet(endpoint).reply(200, [{ id: 1, word: "darn" }]);
    axiosMock.onDelete("/api/admin/bannedwords").reply(404, {
      message: "Word not found",
    });

    renderPage();

    fireEvent.click(await screen.findByRole("button", { name: "Delete darn" }));

    await waitFor(() => {
      expect(mockToast).toHaveBeenCalledWith("Word not found");
    });

    expect(screen.getByText("darn")).toBeInTheDocument();
  });
});
