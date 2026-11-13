import { getPlayPageBackground } from "main/utils/playPageBackgroundUtils";

describe("getPlayPageBackground", () => {
  test.each([
    [100, "PlayPageBackground-Smiling"],
    [75, "PlayPageBackground-Smiling"],
    [74.99, "PlayPageBackground-Neutral"],
    [50, "PlayPageBackground-Neutral"],
    [49.99, "PlayPageBackground-Frowning"],
    [25, "PlayPageBackground-Frowning"],
    [24.99, "PlayPageBackground-Dead"],
    [0, "PlayPageBackground-Dead"],
    [-3, "PlayPageBackground-Dead"],
  ])("health %p uses %s", (health, name) => {
    expect(getPlayPageBackground(health)).toContain(name);
  });

  test.each([[undefined], [null], [NaN], ["abc"]])(
    "unknown health %p uses the original background",
    (health) => {
      const url = getPlayPageBackground(health);
      expect(url).toContain("PlayPageBackground.jpg");
    },
  );
});
