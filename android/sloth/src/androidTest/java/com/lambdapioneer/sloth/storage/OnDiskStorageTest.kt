package com.lambdapioneer.sloth.storage

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.lambdapioneer.sloth.SlothStorageKeyNotFound
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatExceptionOfType
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class OnDiskStorageTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val instance = OnDiskStorage(context)

    @Test
    fun testDiskStorage_clearAll() {
        instance.basePath().deleteRecursively()
    }

    @Test
    fun testDiskStorage_whenPutKey_thenGetRetrieves() {
        val value = "hello".toByteArray()
        instance.put("key", value)

        val actual = instance.get("key")
        assertThat(actual).isEqualTo(value)
    }

    @Test(expected = SlothStorageKeyNotFound::class)
    fun testDiskStorage_whenPutKeyFollowedByClearAll_thenGetThrows() {
        val value = "hello".toByteArray()
        instance.put("key", value)
        instance.basePath().deleteRecursively()
        instance.get("key")
    }

    @Test
    fun testDiskStorage_whenUsingNameSpaces_thenPutAndDeleteVisibleAcrossInstances() {
        val namespace = "test_ondiskstorage_namespace"

        // create a new namespace with two keys
        val instance1 = OnDiskStorage(context)
        val instance1namespaced = instance1.getOrCreateNamespace(namespace)
        instance1namespaced.put("key1", "a".encodeToByteArray())
        instance1namespaced.put("key2", "b".encodeToByteArray())

        // retrieve the same key using a new instance under the same namespace
        val instance2 = OnDiskStorage(context)
        val instance2namespaced = instance2.getOrCreateNamespace(namespace)
        assertThat(instance2namespaced.get("key1")).isEqualTo("a".encodeToByteArray())
        assertThat(instance2namespaced.get("key2")).isEqualTo("b".encodeToByteArray())

        // retrieving the same key using a new instance under a different namespace fails
        val instance3 = OnDiskStorage(context)
        val instance3namespaced = instance3.getOrCreateNamespace("test_different_namespace")
        assertThatExceptionOfType(SlothStorageKeyNotFound::class.java).isThrownBy {
            instance3namespaced.get("key1")
        }

        // deleting the key in instance1 also means that the key is not in instance2
        instance1namespaced.delete("key1")
        assertThat(instance2namespaced.get("key2")).isEqualTo("b".encodeToByteArray())
        assertThatExceptionOfType(SlothStorageKeyNotFound::class.java).isThrownBy {
            instance1namespaced.get("key1")
        }
        assertThatExceptionOfType(SlothStorageKeyNotFound::class.java).isThrownBy {
            instance2namespaced.get("key1")
        }

        // deleting the entire namespace from instance2 means that also instance1 has no more keys
        instance2.deleteNamespace(namespace)
        assertThatExceptionOfType(SlothStorageKeyNotFound::class.java).isThrownBy {
            instance1namespaced.get("key2")
        }
        assertThatExceptionOfType(SlothStorageKeyNotFound::class.java).isThrownBy {
            instance2namespaced.get("key2")
        }
    }

    @Suppress("LocalVariableName")
    @Test
    fun testDiskStorage_whenUpdatingLastModifiedTimestamps_thenAllFilesAreAffected() {
        val namespace = "test_timestamps"

        val instance = OnDiskStorage(context).getOrCreateNamespace(namespace)
        instance.put("key1", "a".encodeToByteArray())
        instance.put("key2", "b".encodeToByteArray())

        val rootBefore = instance.basePath().lastModified()
        val Key1Before = File(instance.basePath(), "key1").lastModified()
        val Key2Before = File(instance.basePath(), "key2").lastModified()

        Thread.sleep(1001)
        instance.updateAllLastModifiedTimestamps()

        val rootAfter = instance.basePath().lastModified()
        val Key1After = File(instance.basePath(), "key1").lastModified()
        val Key2After = File(instance.basePath(), "key2").lastModified()

        assertThat(rootAfter).isGreaterThan(rootBefore)
        assertThat(Key1After).isGreaterThan(Key1Before)
        assertThat(Key2After).isGreaterThan(Key2Before)
    }
}
