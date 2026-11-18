import HerdList from "main/components/Game/HerdList";
import herdFixtures from "fixtures/herdFixtures";

export default {
  title: "components/Game/HerdList",
  component: HerdList,
  args: { onRetry: () => {} },
};

export const SeveralHerds = { args: { herds: herdFixtures.severalHerds } };
export const OneHerd = { args: { herds: [herdFixtures.severalHerds[0]] } };
export const Empty = { args: { herds: [] } };
export const Loading = { args: { isLoading: true } };
export const Error = { args: { isError: true } };
