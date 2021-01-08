package org.gotson.komga.infrastructure.jooq

import mu.KotlinLogging
import org.assertj.core.api.Assertions.assertThat
import org.gotson.komga.domain.model.Book
import org.gotson.komga.domain.model.BookSearch
import org.gotson.komga.domain.model.makeBook
import org.gotson.komga.domain.model.makeLibrary
import org.gotson.komga.domain.model.makeSeries
import org.gotson.komga.domain.persistence.LibraryRepository
import org.gotson.komga.domain.persistence.SeriesRepository
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.net.URL
import java.time.LocalDateTime

@ExtendWith(SpringExtension::class)
@SpringBootTest
class BookDaoTest(
  @Autowired private val bookDao: BookDao,
  @Autowired private val seriesRepository: SeriesRepository,
  @Autowired private val libraryRepository: LibraryRepository
) {

  private val library = makeLibrary()
  private val series = makeSeries("Series")

  @BeforeAll
  fun setup() {
    libraryRepository.insert(library)
    seriesRepository.insert(series.copy(libraryId = library.id))
  }

  @AfterEach
  fun deleteBooks() {
    bookDao.deleteAll()
    assertThat(bookDao.count()).isEqualTo(0)
  }

  @AfterAll
  fun tearDown() {
    seriesRepository.deleteAll()
    libraryRepository.deleteAll()
  }

  @Test
  fun `given a book when inserting then it is persisted`() {
    val now = LocalDateTime.now()
    val book = Book(
      name = "Book",
      url = URL("file://book"),
      fileLastModified = now,
      fileSize = 3,
      seriesId = series.id,
      libraryId = library.id
    )

    bookDao.insert(book)
    val created = bookDao.findByIdOrNull(book.id)!!

    assertThat(created.id).isNotEqualTo(0)
    assertThat(created.createdDate).isCloseTo(now, offset)
    assertThat(created.lastModifiedDate).isCloseTo(now, offset)
    assertThat(created.name).isEqualTo(book.name)
    assertThat(created.url).isEqualTo(book.url)
    assertThat(created.fileLastModified).isEqualToIgnoringNanos(book.fileLastModified)
    assertThat(created.fileSize).isEqualTo(book.fileSize)
  }

  @Test
  fun `given existing book when updating then it is persisted`() {
    val book = Book(
      name = "Book",
      url = URL("file://book"),
      fileLastModified = LocalDateTime.now(),
      fileSize = 3,
      seriesId = series.id,
      libraryId = library.id
    )
    bookDao.insert(book)

    val modificationDate = LocalDateTime.now()

    val updated = with(bookDao.findByIdOrNull(book.id)!!) {
      copy(
        name = "Updated",
        url = URL("file://updated"),
        fileLastModified = modificationDate,
        fileSize = 5
      )
    }

    bookDao.update(updated)
    val modified = bookDao.findByIdOrNull(updated.id)!!

    assertThat(modified.id).isEqualTo(updated.id)
    assertThat(modified.createdDate).isEqualTo(updated.createdDate)
    assertThat(modified.lastModifiedDate)
      .isCloseTo(modificationDate, offset)
      .isNotEqualTo(updated.lastModifiedDate)
    assertThat(modified.name).isEqualTo("Updated")
    assertThat(modified.url).isEqualTo(URL("file://updated"))
    assertThat(modified.fileLastModified).isEqualToIgnoringNanos(modificationDate)
    assertThat(modified.fileSize).isEqualTo(5)
  }

  @Test
  fun `given existing book when finding by id then book is returned`() {
    val book = Book(
      name = "Book",
      url = URL("file://book"),
      fileLastModified = LocalDateTime.now(),
      fileSize = 3,
      seriesId = series.id,
      libraryId = library.id
    )
    bookDao.insert(book)

    val found = bookDao.findByIdOrNull(book.id)

    assertThat(found).isNotNull
    assertThat(found?.name).isEqualTo("Book")
  }

  @Test
  fun `given non-existing book when finding by id then null is returned`() {
    val found = bookDao.findByIdOrNull("128742")

    assertThat(found).isNull()
  }

  @Test
  fun `given some books when finding all then all are returned`() {
    bookDao.insert(makeBook("1", libraryId = library.id, seriesId = series.id))
    bookDao.insert(makeBook("2", libraryId = library.id, seriesId = series.id))

    val found = bookDao.findAll()

    assertThat(found).hasSize(2)
  }

  @Test
  fun `given some books when searching then results are returned`() {
    bookDao.insert(makeBook("1", libraryId = library.id, seriesId = series.id))
    bookDao.insert(makeBook("2", libraryId = library.id, seriesId = series.id))

    val search = BookSearch(
      libraryIds = listOf(library.id),
      seriesIds = listOf(series.id)
    )
    val found = bookDao.findAll(search)

    assertThat(found).hasSize(2)
  }

  @Test
  fun `given some books when finding by libraryId then results are returned`() {
    bookDao.insert(makeBook("1", libraryId = library.id, seriesId = series.id))
    bookDao.insert(makeBook("2", libraryId = library.id, seriesId = series.id))

    val found = bookDao.findAllIdByLibraryId(library.id)

    assertThat(found).hasSize(2)
  }

  @Test
  fun `given some books when finding by seriesId then results are returned`() {
    bookDao.insert(makeBook("1", libraryId = library.id, seriesId = series.id))
    bookDao.insert(makeBook("2", libraryId = library.id, seriesId = series.id))

    val found = bookDao.findAllIdBySeriesId(series.id)

    assertThat(found).hasSize(2)
  }

  @Test
  fun `given some books when deleting all then count is zero`() {
    bookDao.insert(makeBook("1", libraryId = library.id, seriesId = series.id))
    bookDao.insert(makeBook("2", libraryId = library.id, seriesId = series.id))

    bookDao.deleteAll()

    assertThat(bookDao.count()).isEqualTo(0)
  }

  private val logger = KotlinLogging.logger {}

//  @Test
//  fun benchmark() {
//    val books = (1..10000).map {
//      makeBook(it.toString(), libraryId = library.id, seriesId = series.id)
//    }
//
//    val single = measureTime {
//      books.map { bookDao.insert(it) }
//    }
//    bookDao.deleteAll()
//
//    val singleBatch = measureTime {
//      books.map { bookDao.insertBatch(it) }
//    }
//    bookDao.deleteAll()
//
//    val transaction = measureTime {
//      bookDao.insertMany(books)
//    }
//    bookDao.deleteAll()
//
//    logger.info { "Single: $single" }
//    logger.info { "SingleBatch: $singleBatch" }
//    logger.info { "Transaction: $transaction" }
//
//    assertThat(single).isEqualTo(transaction)
//  }
}
