package com.example.budget.data.repository

import com.example.budget.data.db.dao.PersonDao
import com.example.budget.data.db.entity.PersonEntity
import kotlinx.coroutines.flow.Flow

class PersonRepository(private val personDao: PersonDao) {

    suspend fun addPerson(name: String) {
        val person = PersonEntity(name = name)
        personDao.insertPerson(person)
    }

    suspend fun addPersonAndGetId(name: String): Long {
        val existing = personDao.getPersonByName(name)
        if (existing != null) return existing.id
        
        val person = PersonEntity(name = name)
        return personDao.insertPerson(person)
    }

    suspend fun updatePerson(person: PersonEntity) {
        personDao.updatePerson(person)
    }

    suspend fun mergePeople(sourcePersonId: Long, targetPersonId: Long) {
        val sourcePerson = personDao.getPersonById(sourcePersonId)
        sourcePerson?.let {
            personDao.updatePerson(
                it.copy(
                    isMerged = true,
                    mergedIntoPersonId = targetPersonId
                )
            )
        }
    }

    fun getActivePeople(): Flow<List<PersonEntity>> {
        return personDao.getAllActivePeople()
    }

    suspend fun getPersonById(id: Long): PersonEntity? {
        return personDao.getPersonById(id)
    }
}
