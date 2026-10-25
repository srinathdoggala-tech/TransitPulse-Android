const { initializeTestEnvironment, assertFails, assertSucceeds } = require('@firebase/rules-unit-testing');
const fs = require('fs');

let testEnv;

describe('Firestore Security Rules', () => {
    before(async () => {
        testEnv = await initializeTestEnvironment({
            projectId: 'habittracker-test',
            firestore: { rules: fs.readFileSync('firestore.rules', 'utf8') }
        });
    });

    after(async () => {
        await testEnv.cleanup();
    });

    beforeEach(async () => {
        await testEnv.clearFirestore();
    });

    it('should reject oversized habit name', async () => {
        const alice = testEnv.authenticatedContext('alice');
        const db = alice.firestore();
        
        const oversizedName = 'a'.repeat(200); // Limit is 150
        
        await assertFails(db.collection('users').doc('alice').collection('habits').doc('habit1').set({
            name: oversizedName
        }));
    });

    it('should accept valid habit', async () => {
        const alice = testEnv.authenticatedContext('alice');
        const db = alice.firestore();
        
        await assertSucceeds(db.collection('users').doc('alice').collection('habits').doc('habit1').set({
            name: 'Morning Routine',
            description: 'Starts the day right'
        }));
    });
    
    it('should reject oversized description', async () => {
        const alice = testEnv.authenticatedContext('alice');
        const db = alice.firestore();
        
        const oversizedDesc = 'a'.repeat(1500); // Limit is 1000
        
        await assertFails(db.collection('users').doc('alice').collection('habits').doc('habit1').set({
            name: 'Routine',
            description: oversizedDesc
        }));
    });
});
