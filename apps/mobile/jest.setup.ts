process.env.RNTL_SKIP_DEPS_CHECK = "true";

jest.mock("@react-native-async-storage/async-storage", () =>
  require("../../node_modules/@react-native-async-storage/async-storage/src/jest/AsyncStorageMock"),
);
