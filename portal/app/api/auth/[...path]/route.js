import { auth } from '../../../../lib/auth.js';

export const GET = (...args) => auth().handler().GET(...args);
export const POST = (...args) => auth().handler().POST(...args);
export const PATCH = (...args) => auth().handler().PATCH(...args);
export const PUT = (...args) => auth().handler().PUT(...args);
export const DELETE = (...args) => auth().handler().DELETE(...args);
