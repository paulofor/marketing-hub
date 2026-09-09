#!/usr/bin/env node
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';

// Modelo simulado exclusivo da homologação, sem rede, credencial ou inferência real.
const args = process.argv.slice(2);
const output = args[args.indexOf('--output-last-message') + 1];
const state = path.dirname(path.dirname(output));
const prompt = fs.readFileSync(0, 'utf8');
const audit = JSON.parse(fs.readFileSync(path.join(state, 'audit.json'), 'utf8'));
if (audit.promptSent !== prompt || !prompt.includes('experiment:92')) process.exit(2);
fs.appendFileSync(path.join(state, 'invocations.txt'), 'call\n');
fs.copyFileSync(path.join(path.dirname(fileURLToPath(import.meta.url)), 'approved.json'), output);
console.log(JSON.stringify({ type: 'turn.completed', usage: { input_tokens: 100, cached_input_tokens: 20, output_tokens: 10 } }));
