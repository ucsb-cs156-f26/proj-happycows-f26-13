import SellHerdsModal from "main/components/Game/SellHerdsModal";
import herdFixtures from "fixtures/herdFixtures";
import { action } from "storybook/actions";

export default {
  title: "components/Game/SellHerdsModal",
  component: SellHerdsModal,
  args: {
    isOpen: true,
    game: { cowPrice: 100, sellingFee: 2 },
    onClose: action("onClose"),
    onSell: action("onSell"),
  },
};

export const SeveralHerds = { args: { herds: herdFixtures.severalHerds } };
export const OneHerd = { args: { herds: [herdFixtures.severalHerds[0]] } };
export const NoHerds = { args: { herds: [] } };
