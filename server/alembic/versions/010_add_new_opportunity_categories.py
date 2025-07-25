"""add_new_opportunity_categories

Revision ID: 010
Revises: 009
Create Date: 2025-07-23 09:30:00.000000

"""
from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision = '010'
down_revision = '009'
branch_labels = None
depends_on = None


def upgrade():
    # Update the enum type with new categories
    # Note: This is a simplified approach. In production, you might need to handle this differently
    # depending on your database constraints and existing data.
    
    # Add new enum values to the category column
    # PostgreSQL requires special handling for enum modifications
    op.execute("ALTER TYPE opportunitycategory ADD VALUE IF NOT EXISTS 'food_security'")
    op.execute("ALTER TYPE opportunitycategory ADD VALUE IF NOT EXISTS 'animal_welfare'")
    op.execute("ALTER TYPE opportunitycategory ADD VALUE IF NOT EXISTS 'arts_culture'")
    op.execute("ALTER TYPE opportunitycategory ADD VALUE IF NOT EXISTS 'youth_mentoring'")
    op.execute("ALTER TYPE opportunitycategory ADD VALUE IF NOT EXISTS 'elderly_care'")
    op.execute("ALTER TYPE opportunitycategory ADD VALUE IF NOT EXISTS 'technology'")


def downgrade():
    # Note: PostgreSQL doesn't support removing enum values easily
    # In a real production environment, you would need to handle this more carefully
    # For this migration, we'll leave the enum values in place as removing them
    # could cause issues if any data uses these values
    pass
