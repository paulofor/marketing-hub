#!/usr/bin/env node
import { readFileSync, writeFileSync } from 'node:fs';
import { join } from 'node:path';

const args = process.argv.slice(2);
const value = (flag) => args[args.indexOf(flag) + 1];
const directory = value('--cd');
writeFileSync(join(directory, 'prompt.txt'), readFileSync(0));
writeFileSync(join(directory, 'schema.json'), readFileSync(value('--output-schema')));
writeFileSync(value('--output-last-message'), readFileSync(join(directory, 'response.json')));
process.stdout.write(JSON.stringify({ usage: { input_tokens: 7, cached_input_tokens: 0, output_tokens: 3 } }) + '\n');
