let savedArticles = []; // Store all articles
let totalSavedArticles = 0; // Total number of saved articles
const articlesPerPage = 5;

document.addEventListener("DOMContentLoaded", async function () {
    await fetchUserProfile(); // Load profile if authenticated

    document.getElementById("logout-btn").addEventListener("click", logoutUser);
    document.getElementById("update-info-form").addEventListener("submit", updateProfileInfo);
    document.getElementById("update-avatar-form").addEventListener("submit", updateAvatarLink);
    document.getElementById("new-avatar").addEventListener("change", handleAvatarUpload);
    document.getElementById("delete-account-btn").addEventListener("click", deleteUser);
    document.getElementById("show-saved-articles-btn").addEventListener("click", async () => {
        await loadSavedArticles(); // Fetch and show modal
        showSavedArticlesModal(1);
    });

    document.getElementById("close-saved-articles-btn").addEventListener("click", () => {
        document.getElementById("saved-articles-modal").style.display = "none";
    });

});

/* Fetch and display user profile */
async function fetchUserProfile() {
    try {
        const response = await fetch("/api/user/profile", { credentials: "include" });
        if (!response.ok) throw new Error("Unauthorized");

        const user = await response.json();
        document.getElementById("username").textContent = user.userName || "No Name";
        document.getElementById("avatar").src = user.avatarLink || "/images/default-avatar.png";
        document.getElementById("bio").textContent = user.bio || "No bio available.";
    } catch (error) {
        console.error("Error fetching profile:", error);
    }
}

/* Logout */
async function logoutUser() {
    await fetch("/api/logout", { method: "POST", credentials: "include" });
    alert("Logged out successfully.");
    window.location.href = "http://localhost:6999/index.html";
}

/* Update profile info (username & bio) */
async function updateProfileInfo(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    const data = Object.fromEntries(formData.entries());

    const response = await fetch("/api/user/update-info", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(data),
        credentials: "include",
    });

    if (response.ok) {
        alert("Profile info updated.");
        fetchUserProfile();
    } else {
        const errorText = await response.text();
        alert("Failed to update info: " + errorText);
    }
}

/* Upload avatar + update avatar link in backend */
async function handleAvatarUpload(event) {
    const file = event.target.files[0];
    if (!file) return;

    // Step 1: Upload the avatar to the server (store the file, but don't update the avatar URL yet)
    const formData = new FormData();
    formData.append("avatar", file);

    try {
        const uploadResponse = await fetch("/api/avatar/upload", {
            method: "POST",
            body: formData,
        });

        if (!uploadResponse.ok) {
            throw new Error("Failed to upload avatar.");
        }

        const result = await uploadResponse.json();
        const avatarUrl = result.avatarUrl;

        // Display the uploaded image as a preview
        document.getElementById("avatar-preview").src = avatarUrl;

        // Store the avatar URL temporarily in a hidden input field
        document.getElementById("avatarLink").value = avatarUrl;
    } catch (error) {
        alert("Error: " + error.message);
    }
}

async function updateAvatarLink(event) {
    event.preventDefault();

    const avatarUrl = document.getElementById("avatarLink").value;

    if (!avatarUrl) {
        alert("Please upload a new avatar first.");
        return;
    }

    // Step 2: Send the avatar link to update the user's profile
    const response = await fetch("/api/user/avatar", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ avatarLink: avatarUrl }),
        credentials: "include",
    });

    if (response.ok) {
        document.getElementById("avatar").src = avatarUrl; // Update the avatar in the UI
        alert("Avatar updated successfully.");
    } else {
        const errorText = await response.text();
        alert("Failed to update avatar: " + errorText);
    }
}


/* Change password */
async function changePassword(event) {
    event.preventDefault();

    const formData = new FormData(event.target);
    const data = Object.fromEntries(formData.entries());

    // Validate form fields
    if (!data.oldPassword || !data.newPassword || !data.confirmPassword) {
        alert("Please fill in all fields.");
        return;
    }

    if (data.newPassword !== data.confirmPassword) {
        alert("New password and confirmation password do not match.");
        return;
    }

    const payload = {
        currentPassword: data.oldPassword,
        newPassword: data.newPassword
    };

    try {
        const response = await fetch("/api/user/change-password", {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(payload),
            credentials: "include",
        });

        if (response.ok) {
            alert("Password changed successfully.");
            event.target.reset(); // Clear form
        } else {
            const errorText = await response.text();
            alert("Password change failed: " + errorText);
        }
    } catch (error) {
        console.error("Error:", error);
        alert("An error occurred while changing password.");
    }
}



/* Delete account */
async function deleteUser() {
    if (!confirm("Are you sure? This will deactivate your account for 30 days before deletion.")) return;

    const response = await fetch("/api/user/delete", {
        method: "DELETE",
        credentials: "include",
    });

    if (response.ok) {
        alert(await response.text());
        window.location.href = "/index.html";
    } else {
        alert("Failed to delete account.");
    }

}

/* SHOW Saved articles */
async function loadSavedArticles(page = 1) {
    try {
        const response = await fetch(`/api/user/saved-articles?page=${page}&size=${articlesPerPage}`, {
            method: "GET",
            credentials: "include",
        });

        if (!response.ok) throw new Error("Failed to load saved articles.");

        const data = await response.json();
        savedArticles = data.articles;
        totalSavedArticles = data.totalCount; // use this for pagination

        showSavedArticlesModal(page);
    } catch (error) {
        console.error("Error loading saved articles:", error);
        document.getElementById("saved-articles-container").innerHTML = "<p>Error loading articles.</p>";
    }
}

function showSavedArticlesModal(page = 1) {
    const container = document.getElementById("saved-articles-container");
    const pagination = document.getElementById("pagination-controls");
    const modal = document.getElementById("saved-articles-modal");

    modal.style.display = "block";
    container.innerHTML = "";
    pagination.innerHTML = "";

    if (!savedArticles || savedArticles.length === 0) {
        container.innerHTML = "<p>No saved articles yet.</p>";
        return;
    }

    savedArticles.forEach(article => {
        const card = document.createElement("div");
        card.className = "article-card";
        card.innerHTML = `
            <h3>${article.title || "Untitled"}</h3>
            <a href="${article.url || "#"}" target="_blank">Read More</a>
        `;
        container.appendChild(card);
    });

    const totalPages = Math.ceil(totalSavedArticles / articlesPerPage);

    for (let i = 1; i <= totalPages; i++) {
        const pageBtn = document.createElement("button");
        pageBtn.textContent = i;
        pageBtn.classList.add("page-button");
        if (i === page) pageBtn.classList.add("active-page");

        pageBtn.addEventListener("click", () => loadSavedArticles(i));
        pagination.appendChild(pageBtn);
    }
}

