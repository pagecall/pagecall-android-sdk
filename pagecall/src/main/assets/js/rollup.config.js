/* eslint-disable import/no-extraneous-dependencies */
import { nodeResolve } from "@rollup/plugin-node-resolve";
import typescript from "@rollup/plugin-typescript";
import { terser } from "rollup-plugin-terser";

/**
 * @type {import('rollup').RollupOptions}
 */
const config = {
  input: "src/PagecallNative.ts",
  output: [
    {
      sourcemap: true,
      format: "cjs",
      entryFileNames: "index.js",
      dir: "./dist",
    },
  ],
  plugins: [nodeResolve(), typescript(), terser()],
};

export default config;
