import React from "react";
import GameSelect from "main/components/Game/GameSelect";
import gameFixtures from "fixtures/gameFixtures";

export default {
  title: "components/Game/GameSelect",
  component: GameSelect,
};

const Template = (args) => {
  const [selectedGame, setSelectedGame] = React.useState(args.selectedGame);

  const handleGameSelection = (gameId, gameName) => {
    setSelectedGame(gameId);
    args.handleGameSelection(gameId, gameName);
  };

  return (
    <GameSelect
      {...args}
      selectedGame={selectedGame}
      handleGameSelection={handleGameSelection}
    />
  );
};

export const ThreeGames = Template.bind({});

ThreeGames.args = {
  game: gameFixtures.threeGame,
  handleGameSelection: (id, name) =>
    console.log(`Selected game: ${name} (ID: ${id})`),
  testid: "GameSelect",
};
