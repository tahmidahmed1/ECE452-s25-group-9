"""Add default badges

Revision ID: 009_add_default_badges
Revises: 008_add_lost_found_tables
Create Date: 2025-01-21 14:00:00.000000

"""
from alembic import op
import sqlalchemy as sa

# revision identifiers, used by Alembic.
revision = '009_add_default_badges'
down_revision = '008_add_lost_found_tables'
branch_labels = None
depends_on = None


def upgrade() -> None:
    # Insert default badges
    op.execute("""
        INSERT INTO badges (name, description, required_karma_points, icon_name, color, is_active) VALUES
        ('Newcomer', 'Welcome to the community! Complete your first volunteer activity.', 100, 'Star', '#FFD700', true),
        ('Helper', 'You\'re making a difference! Complete 5 volunteer activities.', 500, 'Favorite', '#FF6B35', true),
        ('Dedicated Volunteer', 'Your commitment shines! Complete 10 volunteer activities.', 1000, 'WorkspacePremium', '#4285F4', true),
        ('Community Champion', 'You\'re a pillar of the community! Complete 25 volunteer activities.', 2500, 'EmojiEvents', '#34A853', true),
        ('Volunteer Hero', 'Your impact is incredible! Complete 50 volunteer activities.', 5000, 'Shield', '#9C27B0', true),
        ('Karma Master', 'You\'ve achieved true volunteer mastery! Complete 100 volunteer activities.', 10000, 'Psychology', '#FF5722', true)
    """)


def downgrade() -> None:
    # Remove default badges
    op.execute("""
        DELETE FROM badges WHERE name IN (
            'Newcomer', 'Helper', 'Dedicated Volunteer', 
            'Community Champion', 'Volunteer Hero', 'Karma Master'
        )
    """)
